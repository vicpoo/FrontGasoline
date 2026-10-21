package org.vicpoo.gasolineproject.feature.reservations.domain

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.core.common.AppResult

interface ReservationRepository {
    /** Solicitar un carro. Requiere conexión: la cola de confirmación vive en el servidor. */
    suspend fun requestVehicle(vehicleServerId: String, reason: String): AppResult<Unit>

    /** Mis solicitudes guardadas en el teléfono (offline-first), de la más reciente a la más antigua. */
    fun observeMine(): Flow<List<ReservationRequest>>

    /** Las vuelve a pedir a la API y actualiza la copia del teléfono. */
    suspend fun refreshMine(): AppResult<Unit>

    /** Solicitudes de toda la empresa (pestaña llave), copia del teléfono. */
    fun observeCompany(): Flow<List<ReservationRequest>>

    suspend fun refreshCompany(): AppResult<Unit>

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
