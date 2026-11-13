package org.vicpoo.gasolineproject.domain.reservations

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.RequestOutcome
import org.vicpoo.gasolineproject.domain.reservations.RequestedVehicle

interface ReservationRepository {
    /** Solicitar un carro. Sin conexión queda en cola en el teléfono y se envía sola ([RequestOutcome.QUEUED]). */
    suspend fun requestVehicle(vehicle: RequestedVehicle, reason: String): AppResult<RequestOutcome>

    /** Mis solicitudes guardadas en el teléfono (offline-first), de la más reciente a la más antigua. */
    fun observeMine(): Flow<List<ReservationRequest>>

    /** Las vuelve a pedir a la API y actualiza la copia del teléfono. */
    suspend fun refreshMine(): AppResult<Unit>

    /** "Mis solicitudes": la API tiene más después de las guardadas / pedir las 10 siguientes. */
    fun observeMineHasMore(): Flow<Boolean>
    suspend fun loadMoreMine(): AppResult<Unit>

    /** Solicitudes de toda la empresa (pestaña llave), copia del teléfono. */
    fun observeCompany(): Flow<List<ReservationRequest>>

    suspend fun refreshCompany(): AppResult<Unit>

    /** Solicitudes de la empresa: la API tiene más después de las guardadas / pedir las 10 siguientes. */
    fun observeCompanyHasMore(): Flow<Boolean>
    suspend fun loadMoreCompany(): AppResult<Unit>

    /** Una solicitud de la copia del teléfono (para abrir el detalle sin red). */
    fun observeCompanyRequest(reservationId: String): Flow<ReservationRequest?>

    /** Detalle completo (motivo, licencia del solicitante, carro y quién la resolvió). Requiere red. */
    suspend fun getDetail(reservationId: String): AppResult<ReservationDetail>

    /** Aceptar / rechazar: requieren conexión (la decisión la valida el servidor). */
    suspend fun approve(reservationId: String): AppResult<Unit>
    suspend fun reject(reservationId: String, reason: String): AppResult<Unit>

    /** Campana: cuántas respuestas a mis solicitudes no he visto (aceptada / rechazada). */
    fun observeUnseenUpdates(): Flow<Int>
    suspend fun refreshUnseenUpdates(): AppResult<Unit>

    /** Al abrir la campana se marcan como vistas. */
    suspend fun markUpdatesSeen(): AppResult<Unit>

    /** Al cerrar sesión. */
    suspend fun clear()
}
