package org.vicpoo.gasolineproject.feature.reservations.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.httpCode
import org.vicpoo.gasolineproject.core.common.serverMessage
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.core.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.core.sync.PendingSyncTask
import org.vicpoo.gasolineproject.core.sync.SyncRejection
import org.vicpoo.gasolineproject.core.sync.stopsSync
import org.vicpoo.gasolineproject.feature.reservations.data.local.ReservationOutbox
import org.vicpoo.gasolineproject.feature.reservations.data.remote.CreateReservationRequestDto
import org.vicpoo.gasolineproject.feature.reservations.data.remote.ReservationApi

/**
 * Envía las solicitudes que se hicieron sin conexión.
 * - Sin red / sesión vencida: se detiene y quedan en cola (pide reintentar al volver la red).
 * - Rechazo de la API (ej. 409 "ya tienes una solicitud para ese carro" o el carro ya no está
 *   disponible): se guarda el mensaje en esa solicitud ("No se pudo enviar") y no se reenvía sola.
 * - Error temporal (5xx): se reintenta en la siguiente vuelta.
 * - Si se envió alguna, se vuelve a leer "Mis solicitudes" ([onSent]) para que aparezca ya con su estado real.
 */
class ReservationOutboxSyncer(
    private val outbox: ReservationOutbox,
    private val api: ReservationApi,
    private val sessionReader: SessionReader,
    private val scheduler: BackgroundSyncScheduler,
    private val onSent: suspend () -> Unit,
) : PendingSyncTask {

    private val mutex = Mutex()

    override suspend fun syncPending(): AppResult<Unit> = mutex.withLock {
        val session = sessionReader.current() ?: return@withLock AppResult.Success(Unit)
        val employeeId = session.employeeId ?: return@withLock AppResult.Success(Unit)
        val companyId = session.companyId ?: return@withLock AppResult.Success(Unit)

        var sentAny = false
        try {
            for (item in outbox.all().filterNot { SyncRejection.isPermanent(it.errorCode) }) {
                val request = CreateReservationRequestDto(
                    empresaId = companyId,
                    carroId = item.vehicleServerId,
                    empleadoSolicitanteId = employeeId,
                    motivo = item.reason,
                )
                when (val result = safeApiCall { api.create(request) }) {
                    is AppResult.Success -> {
                        outbox.remove(item.localId)
                        sentAny = true
                    }
                    is AppResult.Failure -> {
                        if (result.error.stopsSync()) {
                            if (result.error == AppError.NoConnection) scheduler.requestWhenOnline()
                            return@withLock result
                        }
                        outbox.markError(item.localId, result.error.httpCode() ?: -1, result.error.serverMessage())
                    }
                }
            }
        } finally {
            if (sentAny) onSent()
        }
        AppResult.Success(Unit)
    }
}
