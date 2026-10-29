package org.vicpoo.gasolineproject.feature.reservations.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.core.common.AppClock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.core.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.core.sync.SyncRejection
import org.vicpoo.gasolineproject.feature.reservations.api.RequestOutcome
import org.vicpoo.gasolineproject.feature.reservations.api.RequestedVehicle
import org.vicpoo.gasolineproject.feature.reservations.data.local.CompanyReservationsSnapshot
import org.vicpoo.gasolineproject.feature.reservations.data.local.MyReservationsSnapshot
import org.vicpoo.gasolineproject.feature.reservations.data.local.OutgoingRequestEntity
import org.vicpoo.gasolineproject.feature.reservations.data.local.ReservationOutbox
import org.vicpoo.gasolineproject.feature.reservations.data.local.ReservationRequestEntity
import org.vicpoo.gasolineproject.feature.reservations.data.remote.ApproveReservationRequestDto
import org.vicpoo.gasolineproject.feature.reservations.data.remote.CreateReservationRequestDto
import org.vicpoo.gasolineproject.feature.reservations.data.remote.RejectReservationRequestDto
import org.vicpoo.gasolineproject.feature.reservations.data.remote.ReservationApi
import org.vicpoo.gasolineproject.feature.reservations.data.remote.ReservationCarDto
import org.vicpoo.gasolineproject.feature.reservations.data.remote.ReservationDetailDto
import org.vicpoo.gasolineproject.feature.reservations.data.remote.ReservationRequestDto
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationDetail
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRepository
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRequest
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationStatus

/**
 * Solicitudes de reserva:
 * - Enviar una solicitud con su motivo. Sin red se guarda en [outbox] y se envía sola después
 *   (ReservationOutboxSyncer); mientras tanto aparece en "Mis solicitudes" como "Por enviar".
 * - "Mis solicitudes" (campana) y "Solicitudes de la empresa" (llave) offline-first: se guardan
 *   en el teléfono con el solicitante y el carro (la API ya los manda en cada solicitud).
 * - Detalle, aceptar y rechazar requieren conexión; al decidir se vuelve a leer la lista porque
 *   aceptar rechaza solas las demás pendientes del mismo carro.
 */
class ReservationRepositoryImpl(
    private val api: ReservationApi,
    private val sessionReader: SessionReader,
    private val cache: JsonFileCache<MyReservationsSnapshot>,
    private val companyCache: JsonFileCache<CompanyReservationsSnapshot>,
    private val outbox: ReservationOutbox,
    private val clock: AppClock,
    private val scheduler: BackgroundSyncScheduler,
) : ReservationRepository {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun requestVehicle(vehicle: RequestedVehicle, reason: String): AppResult<RequestOutcome> {
        val vehicleServerId = vehicle.serverId ?: return AppResult.Failure(AppError.NotFound)
        val session = sessionReader.current() ?: return AppResult.Failure(AppError.Unauthorized)
        val employeeId = session.employeeId ?: return AppResult.Failure(AppError.NoCompany)
        val companyId = session.companyId ?: return AppResult.Failure(AppError.NoCompany)
        val result = safeApiCall {
            api.create(
                CreateReservationRequestDto(
                    empresaId = companyId,
                    carroId = vehicleServerId,
                    empleadoSolicitanteId = employeeId,
                    motivo = reason,
                ),
            )
        }
        return when (result) {
            is AppResult.Success -> AppResult.Success(RequestOutcome.SENT)
            is AppResult.Failure -> {
                if (result.error != AppError.NoConnection) return result
                // Sin red: se guarda y se envía sola en cuanto vuelva la conexión
                outbox.enqueue(
                    OutgoingRequestEntity(
                        localId = "out-${Uuid.random()}",
                        vehicleServerId = vehicleServerId,
                        vehicleTitle = vehicle.title,
                        plates = vehicle.plates,
                        photoUrl = vehicle.photoUrl,
                        reason = reason,
                        createdAt = clock.nowLocalIso(),
                    ),
                )
                scheduler.requestWhenOnline()
                AppResult.Success(RequestOutcome.QUEUED)
            }
        }
    }

    // ---------------- Mis solicitudes (campana) ----------------

    override fun observeMine(): Flow<List<ReservationRequest>> =
        combine(cache.data, outbox.items) { snapshot, pending ->
            // Las que esperan la red van primero: son las más recientes
            pending.sortedByDescending { it.createdAt }.map { it.toDomain() } +
                snapshot.requests
                    .map { it.toDomain() }
                    .sortedByDescending { it.requestedAt.orEmpty() }
        }

    override suspend fun refreshMine(): AppResult<Unit> {
        val employeeId = sessionReader.current()?.employeeId
            ?: return AppResult.Failure(AppError.NoCompany)

        val requests = when (val result = safeApiCall { api.getByEmployee(employeeId) }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }

        val current = cache.current()
        val previous = current.requests.associateBy { it.id }
        val cars = loadMissingCars(requests)
        val entities = requests.map { dto -> dto.toEntity(cars[dto.carroId], previous[dto.solicitudReservaId]) }
        cache.save(current.copy(requests = entities))
        return AppResult.Success(Unit)
    }

    override fun observeUnseenUpdates(): Flow<Int> =
        cache.data.map { it.unseenUpdates }.distinctUntilChanged()

    override suspend fun refreshUnseenUpdates(): AppResult<Unit> {
        val employeeId = sessionReader.current()?.employeeId ?: return AppResult.Success(Unit)
        val unread = when (val result = safeApiCall { api.getUnreadReservationResults(employeeId) }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val current = cache.current()
        cache.save(current.copy(unseenUpdates = unread.count { !it.leida }))
        return AppResult.Success(Unit)
    }

    override suspend fun markUpdatesSeen(): AppResult<Unit> {
        val employeeId = sessionReader.current()?.employeeId ?: return AppResult.Success(Unit)
        val unread = when (val result = safeApiCall { api.getUnreadReservationResults(employeeId) }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        // Una por una: así solo se marcan las respuestas a solicitudes (no otras notificaciones)
        coroutineScope {
            unread.filter { !it.leida }
                .map { async { safeApiCall { api.markNotificationRead(it.notificacionId) } } }
                .awaitAll()
        }
        cache.save(cache.current().copy(unseenUpdates = 0))
        return AppResult.Success(Unit)
    }

    // ---------------- Solicitudes de la empresa (pestaña llave) ----------------

    override fun observeCompany(): Flow<List<ReservationRequest>> =
        companyCache.data.map { snapshot ->
            snapshot.requests
                .map { it.toDomain() }
                .sortedByDescending { it.requestedAt.orEmpty() }
        }

    /** La busca en las de la empresa y, si no está, en las propias (el detalle se abre desde ambas listas). */
    override fun observeCompanyRequest(reservationId: String): Flow<ReservationRequest?> =
        combine(companyCache.data, cache.data) { company, mine ->
            (company.requests.firstOrNull { it.id == reservationId } ?: mine.requests.firstOrNull { it.id == reservationId })
                ?.toDomain()
        }

    override suspend fun refreshCompany(): AppResult<Unit> {
        val requests = when (val result = safeApiCall { api.getCompany() }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val previous = companyCache.current().requests.associateBy { it.id }
        val cars = loadMissingCars(requests)
        val entities = requests.map { dto -> dto.toEntity(cars[dto.carroId], previous[dto.solicitudReservaId]) }
        companyCache.save(CompanyReservationsSnapshot(entities))
        return AppResult.Success(Unit)
    }

    override suspend fun getDetail(reservationId: String): AppResult<ReservationDetail> =
        when (val result = safeApiCall { api.getDetail(reservationId) }) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(result.data.toDomain())
        }

    override suspend fun approve(reservationId: String): AppResult<Unit> {
        val employeeId = sessionReader.current()?.employeeId ?: return AppResult.Failure(AppError.NoCompany)
        val result = safeApiCall { api.approve(reservationId, ApproveReservationRequestDto(empleadoConfirmadorId = employeeId)) }
        // Aceptar rechaza solas las demás pendientes del mismo carro: se vuelve a leer toda la lista
        if (result is AppResult.Success) refreshCompany()
        return result
    }

    override suspend fun reject(reservationId: String, reason: String): AppResult<Unit> {
        val employeeId = sessionReader.current()?.employeeId ?: return AppResult.Failure(AppError.NoCompany)
        val result = safeApiCall {
            api.reject(reservationId, RejectReservationRequestDto(empleadoConfirmadorId = employeeId, motivo = reason))
        }
        if (result is AppResult.Success) refreshCompany()
        return result
    }

    override suspend fun clear() {
        cache.clear()
        companyCache.clear()
        outbox.clear()
    }

    /**
     * La API ya manda el carro en cada solicitud. Solo si alguna viene sin él (API vieja) se pide
     * aparte, una vez por carro y en paralelo; si falla queda null y se usa lo guardado.
     */
    private suspend fun loadMissingCars(requests: List<ReservationRequestDto>): Map<String, ReservationCarDto?> = coroutineScope {
        requests.filter { it.carro == null }.map { it.carroId }.distinct()
            .map { id -> async { id to (safeApiCall { api.getVehicle(id) } as? AppResult.Success)?.data } }
            .awaitAll()
            .toMap()
    }
}

// ---------- Mapeos ----------

private fun ReservationRequestDto.toEntity(fallbackCar: ReservationCarDto?, previous: ReservationRequestEntity?): ReservationRequestEntity {
    val car = carro ?: fallbackCar
    return ReservationRequestEntity(
        id = solicitudReservaId,
        vehicleId = carroId,
        vehicleTitle = car?.let { "${it.marca} ${it.modelo}".trim() } ?: previous?.vehicleTitle,
        plates = car?.placas ?: previous?.plates,
        photoUrl = car?.fotoVehiculoUrl ?: previous?.photoUrl,
        status = estado,
        requestedAt = fechaSolicitud,
        resolvedAt = fechaResolucion,
        resolutionReason = motivoResolucion,
        requestReason = motivoSolicitud,
        requesterId = empleadoSolicitanteId,
        requesterName = solicitante?.nombreCompleto ?: previous?.requesterName,
        requesterPhotoUrl = solicitante?.fotoPerfilUrl ?: previous?.requesterPhotoUrl,
    )
}

private fun statusFromApi(status: String): ReservationStatus = when (status.lowercase()) {
    "pendiente" -> ReservationStatus.PENDING
    "aprobada" -> ReservationStatus.APPROVED
    "rechazada" -> ReservationStatus.REJECTED
    "cancelada" -> ReservationStatus.CANCELLED
    "finalizada" -> ReservationStatus.FINISHED
    else -> ReservationStatus.UNKNOWN
}

private fun ReservationRequestEntity.toDomain() = ReservationRequest(
    id = id,
    vehicleId = vehicleId,
    vehicleTitle = vehicleTitle,
    plates = plates,
    photoUrl = photoUrl,
    status = statusFromApi(status),
    requestedAt = requestedAt,
    resolvedAt = resolvedAt,
    resolutionReason = resolutionReason,
    requestReason = requestReason,
    requesterId = requesterId,
    requesterName = requesterName,
    requesterPhotoUrl = requesterPhotoUrl,
)

/** Solicitud en cola: "Por enviar", o "No se pudo enviar" (con el mensaje de la API) si la rechazó. */
private fun OutgoingRequestEntity.toDomain() = ReservationRequest(
    id = localId,
    vehicleId = vehicleServerId,
    vehicleTitle = vehicleTitle,
    plates = plates,
    photoUrl = photoUrl,
    status = if (SyncRejection.isPermanent(errorCode)) ReservationStatus.SEND_FAILED else ReservationStatus.NOT_SENT,
    requestedAt = createdAt,
    resolvedAt = null,
    resolutionReason = errorMessage,
    requestReason = reason,
)

private fun ReservationDetailDto.toDomain(): ReservationDetail {
    val car = carro
    val requester = solicitante
    return ReservationDetail(
        request = ReservationRequest(
            id = solicitudReservaId,
            vehicleId = carroId,
            vehicleTitle = car?.let { "${it.marca} ${it.modelo}".trim() },
            plates = car?.placas,
            photoUrl = car?.fotoVehiculoUrl,
            status = statusFromApi(estado),
            requestedAt = fechaSolicitud,
            resolvedAt = fechaResolucion,
            resolutionReason = motivoResolucion,
            requestReason = motivoSolicitud,
            requesterId = empleadoSolicitanteId,
            requesterName = requester?.nombreCompleto,
            requesterPhotoUrl = requester?.fotoPerfilUrl,
        ),
        requesterHasLicense = requester?.tieneLicencia == true,
        requesterLicenseUrl = requester?.fotoLicenciaUrl,
        requesterLicenseUploadedAt = requester?.fechaCargaLicencia,
        vehicleYear = car?.anio,
        vehicleColor = car?.color,
        vehicleMileageKm = car?.kilometrajeActual,
        vehicleState = car?.estado,
        confirmerName = confirmador?.nombreCompleto,
        confirmerPhotoUrl = confirmador?.fotoPerfilUrl,
    )
}
