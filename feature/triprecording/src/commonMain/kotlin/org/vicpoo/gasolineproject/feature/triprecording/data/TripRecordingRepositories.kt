package org.vicpoo.gasolineproject.feature.triprecording.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.core.common.AppClock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.feature.triprecording.data.local.AssignedVehiclesSnapshot
import org.vicpoo.gasolineproject.feature.triprecording.data.local.CostCentersSnapshot
import org.vicpoo.gasolineproject.feature.triprecording.data.local.FuelEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.FuelSyncState
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.AssignedCarroDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.LiberarReservaRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.TripRecordingApi
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicle
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicleRepository
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenter
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenterRepository
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecord
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecordRepository
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecordValues
import org.vicpoo.gasolineproject.feature.triprecording.domain.ReleaseBlock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Carros asignados = solicitudes APROBADAS del empleado + datos de cada carro.
 * Se guardan en el teléfono para poder iniciar un viaje sin red.
 */
class AssignedVehicleRepositoryImpl(
    private val api: TripRecordingApi,
    private val cache: JsonFileCache<AssignedVehiclesSnapshot>,
    private val sessionReader: SessionReader,
    /** Viajes del teléfono: deciden si un carro se puede dejar. */
    private val store: TripRecordingLocalStore,
    private val clock: AppClock,
) : AssignedVehicleRepository {

    override fun observe(): Flow<List<AssignedVehicle>> =
        cache.data.map { snapshot -> snapshot.vehicles.map { it.toDomain() } }

    override suspend fun refresh(): AppResult<Unit> {
        val employeeId = sessionReader.current()?.employeeId
            ?: return AppResult.Failure(AppError.NoCompany)

        val reservations = when (val result = safeApiCall { api.getApprovedReservations(employeeId) }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data.filter { it.estado.equals("aprobada", ignoreCase = true) }
        }

        // Los carros se piden en paralelo; si uno ya no existe se omite
        val cars: List<AppResult<AssignedCarroDto>> = coroutineScope {
            reservations.map { reservation -> async { safeApiCall { api.getVehicle(reservation.carroId) } } }.awaitAll()
        }
        cars.firstOrNull { it is AppResult.Failure && it.error == AppError.NoConnection }?.let {
            return AppResult.Failure(AppError.NoConnection)
        }

        val vehicles = reservations.zip(cars)
            .mapNotNull { (reservation, car) ->
                val dto = (car as? AppResult.Success)?.data ?: return@mapNotNull null
                if (dto.dadoDeBaja) null else reservation.toAssignedEntity(dto)
            }
            .distinctBy { it.vehicleId }
        cache.save(AssignedVehiclesSnapshot(vehicles))
        return AppResult.Success(Unit)
    }

    override suspend fun find(reservationId: String): AssignedVehicle? =
        cache.current().vehicles.firstOrNull { it.reservationId == reservationId }?.toDomain()

    // Un viaje se borra del teléfono cuando todo llegó a la API: si sigue aquí, está en curso
    // (running) o guardado pendiente de enviar.
    override fun observeReleaseBlocks(): Flow<Map<String, ReleaseBlock>> =
        store.snapshot.map { snapshot ->
            snapshot.trips
                .groupBy { it.vehicle.reservationId }
                .mapValues { (_, trips) -> if (trips.any { it.running }) ReleaseBlock.ACTIVE_TRIP else ReleaseBlock.PENDING_SYNC }
        }.distinctUntilChanged()

    override suspend fun release(reservationId: String, notes: String?): AppResult<Unit> {
        val request = LiberarReservaRequestDto(
            fechaLiberacion = clock.toLocalIso(clock.nowMillis()),
            observaciones = notes,
        )
        val result = safeApiCall { api.releaseReservation(reservationId, request) }
        if (result is AppResult.Success) {
            // Sale de la lista al instante (ya no es suyo); la próxima actualización lo confirma
            val current = cache.current()
            cache.save(current.copy(vehicles = current.vehicles.filterNot { it.reservationId == reservationId }))
        }
        return result
    }

    suspend fun clear() = cache.clear()
}

/** Centros de costo activos de la empresa del usuario (offline). */
class CostCenterRepositoryImpl(
    private val api: TripRecordingApi,
    private val cache: JsonFileCache<CostCentersSnapshot>,
    private val sessionReader: SessionReader,
) : CostCenterRepository {

    override fun observe(): Flow<List<CostCenter>> =
        cache.data.map { snapshot -> snapshot.costCenters.map { it.toDomain() } }

    override suspend fun refresh(): AppResult<Unit> {
        val companyId = sessionReader.current()?.companyId
            ?: return AppResult.Failure(AppError.NoCompany)
        return when (val result = safeApiCall { api.getCostCenters(companyId) }) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val active = result.data.filter { it.activo }.sortedBy { it.nombre.lowercase() }.map { it.toEntity() }
                cache.save(CostCentersSnapshot(active))
                AppResult.Success(Unit)
            }
        }
    }

    suspend fun clear() = cache.clear()
}

/**
 * Recargas de gasolina: siempre se guardan primero en el teléfono.
 * El envío lo hace TripRecordingSyncer (Single Responsibility).
 */
class FuelRecordRepositoryImpl(
    private val store: TripRecordingLocalStore,
    private val clock: AppClock,
) : FuelRecordRepository {

    override fun observeForTrip(tripLocalId: String): Flow<List<FuelRecord>> =
        store.snapshot.map { snapshot ->
            snapshot.fuel
                .filter { it.tripLocalId == tripLocalId && it.state != FuelSyncState.PENDING_DELETE }
                .sortedBy { it.number }
                .map { it.toDomain() }
        }

    override suspend fun get(localId: String): FuelRecord? =
        store.current().fuel.firstOrNull { it.localId == localId && it.state != FuelSyncState.PENDING_DELETE }?.toDomain()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun save(
        tripLocalId: String,
        localId: String?,
        values: FuelRecordValues,
        newReceipt: PickedFile?,
        location: GeoPoint?,
    ): AppResult<FuelRecord> {
        val snapshot = store.current()
        val trip = snapshot.trips.firstOrNull { it.localId == tripLocalId }
            ?: return AppResult.Failure(AppError.NotFound)

        val receiptPath = newReceipt?.let { store.saveReceipt(it.bytes, it.extension()) }

        val saved: FuelEntity = if (localId == null) {
            val created = FuelEntity(
                localId = Uuid.random().toString(),
                tripLocalId = tripLocalId,
                number = (snapshot.fuel.filter { it.tripLocalId == tripLocalId }.maxOfOrNull { it.number } ?: 0) + 1,
                amount = values.amount,
                liters = values.liters,
                costCenter = values.costCenter?.toEntity(),
                receiptPath = receiptPath,
                receiptMimeType = newReceipt?.mimeType,
                receiptChanged = receiptPath != null,
                loadedAt = clock.nowLocalIso(),
                // La que se tomó al abrir el formulario; si no hubo, la última del viaje
                lat = location?.lat ?: trip.lastLat,
                lng = location?.lng ?: trip.lastLng,
                state = FuelSyncState.PENDING_CREATE,
            )
            store.update { it.copy(fuel = it.fuel + created) }
            created
        } else {
            val previous = snapshot.fuel.firstOrNull { it.localId == localId }
                ?: return AppResult.Failure(AppError.NotFound)
            if (receiptPath != null) store.deleteFile(previous.receiptPath)
            store.updateFuel(localId) { current ->
                current.copy(
                    amount = values.amount,
                    liters = values.liters,
                    costCenter = values.costCenter?.toEntity(),
                    receiptPath = receiptPath ?: current.receiptPath,
                    receiptMimeType = newReceipt?.mimeType ?: current.receiptMimeType,
                    receiptChanged = current.receiptChanged || receiptPath != null,
                    // Si nunca se envió sigue siendo "crear"; si ya estaba en la API es "corregir"
                    state = if (current.serverId == null) FuelSyncState.PENDING_CREATE else FuelSyncState.PENDING_UPDATE,
                    syncErrorMessage = null,
                    syncErrorCode = null,
                )
            } ?: return AppResult.Failure(AppError.NotFound)
        }
        return AppResult.Success(saved.toDomain())
    }

    override suspend fun delete(localId: String): AppResult<Unit> {
        val fuel = store.current().fuel.firstOrNull { it.localId == localId }
            ?: return AppResult.Failure(AppError.NotFound)
        if (fuel.serverId == null) {
            // Nunca llegó a la API: se borra del teléfono y listo
            store.update { snapshot -> snapshot.copy(fuel = snapshot.fuel.filterNot { it.localId == localId }) }
            store.deleteFile(fuel.receiptPath)
        } else {
            store.updateFuel(localId) { it.copy(state = FuelSyncState.PENDING_DELETE, syncErrorMessage = null, syncErrorCode = null) }
        }
        return AppResult.Success(Unit)
    }

    override suspend fun markForRetry(localId: String) {
        val fuel = store.updateFuel(localId) { it.copy(syncErrorMessage = null, syncErrorCode = null) } ?: return
        // Si la recarga no subió porque la API rechazó el viaje, también se vuelve a intentar crear el viaje
        store.updateTrip(fuel.tripLocalId) { trip ->
            if (trip.serverId == null) trip.copy(syncErrorMessage = null, syncErrorCode = null) else trip
        }
    }
}

private fun PickedFile.extension(): String = when (mimeType.lowercase()) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    else -> "jpg"
}
