package org.vicpoo.gasolineproject.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.profile.Profile
import org.vicpoo.gasolineproject.domain.profile.ProfileRepository
import org.vicpoo.gasolineproject.domain.trips.Trip
import org.vicpoo.gasolineproject.domain.trips.TripStatus
import org.vicpoo.gasolineproject.domain.trips.TripsRepository
import org.vicpoo.gasolineproject.domain.vehicles.model.SyncState
import org.vicpoo.gasolineproject.domain.vehicles.model.Vehicle
import org.vicpoo.gasolineproject.domain.vehicles.model.VehicleDraft
import org.vicpoo.gasolineproject.domain.vehicles.model.VehiclePhoto
import org.vicpoo.gasolineproject.domain.vehicles.model.VehicleStatus
import org.vicpoo.gasolineproject.domain.vehicles.repository.VehicleRepository

/* Fakes de vehículos, historial de viajes y perfil (en memoria). */

// ---------------- Vehículos ----------------

fun testVehicle(
    id: String = "veh-1",
    serverId: String? = "car-1",
    status: VehicleStatus = VehicleStatus.AVAILABLE,
    photo: VehiclePhoto? = VehiclePhoto.Remote("http://13.222.72.73/fotos/versa.jpg"),
) = Vehicle(
    id = id,
    serverId = serverId,
    brand = "Nissan",
    model = "Versa",
    plates = "ABC-123",
    year = 2022,
    color = "Blanco",
    mileageKm = 15_000.0,
    tankCapacityLiters = 41.0,
    status = status,
    photo = photo,
    syncState = SyncState.SYNCED,
    syncErrorMessage = null,
)

/** Vehículos en memoria: crear / editar / borrar cambian la lista; [failure] hace fallar la escritura. */
class FakeVehicleRepository(initial: List<Vehicle> = emptyList()) : VehicleRepository {
    val vehicles = MutableStateFlow(initial)
    var failure: AppError? = null
    val created = mutableListOf<VehicleDraft>()
    val updated = mutableListOf<Pair<String, VehicleDraft>>()
    val deleted = mutableListOf<String>()

    override fun observeVehicles(): Flow<List<Vehicle>> = vehicles
    override fun observeVehicle(id: String): Flow<Vehicle?> = vehicles.map { list -> list.firstOrNull { it.id == id } }
    override fun observePendingSyncCount(): Flow<Int> = vehicles.map { list -> list.count { it.isPendingSync } }
    override suspend fun refresh(): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun create(draft: VehicleDraft): AppResult<Vehicle> {
        failure?.let { return AppResult.Failure(it) }
        created += draft
        val vehicle = draft.toVehicle(id = "veh-${vehicles.value.size + 1}", previous = null)
        vehicles.value = vehicles.value + vehicle
        return AppResult.Success(vehicle)
    }

    override suspend fun update(id: String, draft: VehicleDraft): AppResult<Vehicle> {
        failure?.let { return AppResult.Failure(it) }
        updated += id to draft
        val previous = vehicles.value.firstOrNull { it.id == id } ?: return AppResult.Failure(AppError.NotFound)
        val vehicle = draft.toVehicle(id = id, previous = previous)
        vehicles.value = vehicles.value.map { if (it.id == id) vehicle else it }
        return AppResult.Success(vehicle)
    }

    override suspend fun delete(id: String): AppResult<Unit> {
        failure?.let { return AppResult.Failure(it) }
        deleted += id
        vehicles.value = vehicles.value.filterNot { it.id == id }
        return AppResult.Success(Unit)
    }

    private fun VehicleDraft.toVehicle(id: String, previous: Vehicle?) = Vehicle(
        id = id,
        serverId = previous?.serverId,
        brand = brand,
        model = model,
        plates = plates,
        year = year,
        color = color,
        mileageKm = mileageKm,
        tankCapacityLiters = tankCapacityLiters,
        status = previous?.status ?: VehicleStatus.AVAILABLE,
        type = type,
        photo = newPhoto?.let { VehiclePhoto.Local("/fotos/${it.name}") } ?: if (removePhoto) null else previous?.photo,
        syncState = if (previous == null) SyncState.PENDING_CREATE else SyncState.PENDING_UPDATE,
        syncErrorMessage = null,
    )
}

// ---------------- Historial de viajes ----------------

fun testTrip(id: String, startedAt: String = "2026-11-20T08:00:00") = Trip(
    id = id,
    operatorName = "Juan Pérez",
    operatorPhotoUrl = null,
    startedAt = startedAt,
    endedAt = "2026-11-20T10:00:00",
    distanceKm = 42.5,
    finalMileageKm = 15_042.5,
    status = TripStatus.FINISHED,
    fuelLoads = emptyList(),
    route = emptyList(),
)

/**
 * Historial en memoria. [loadMore] agrega [nextPage] (si hay) y apaga [hasMore].
 * [detailRequests] guarda cada bloque de ids que se pidió (cargas y ruta).
 */
class FakeTripsRepository(initial: List<Trip> = emptyList()) : TripsRepository {
    val trips = MutableStateFlow(initial)
    val hasMore = MutableStateFlow(false)
    var nextPage: List<Trip> = emptyList()
    var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
    var loadMoreResult: AppResult<Unit> = AppResult.Success(Unit)
    var refreshCalls = 0
    var loadMoreCalls = 0
    val detailRequests = mutableListOf<List<String>>()

    override fun observeTrips(): Flow<List<Trip>> = trips
    override fun observeTrip(id: String): Flow<Trip?> = trips.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refresh(): AppResult<Unit> {
        refreshCalls++
        return refreshResult
    }

    override fun observeHasMore(): Flow<Boolean> = hasMore

    override suspend fun loadMore(): AppResult<Unit> {
        loadMoreCalls++
        if (loadMoreResult is AppResult.Success) {
            trips.value = trips.value + nextPage
            nextPage = emptyList()
            hasMore.value = false
        }
        return loadMoreResult
    }

    override suspend fun loadDetails(tripIds: List<String>): AppResult<Unit> {
        detailRequests += tripIds
        return AppResult.Success(Unit)
    }
}

// ---------------- Perfil ----------------

fun testProfile(name: String = "Juan Pérez") = Profile(
    name = name,
    email = "test@polarsoft.mx",
    photoUrl = null,
    roles = listOf("Operador"),
    hasEmployee = true,
)

/** Perfil en memoria: cambiar el nombre o la foto actualiza [profile]. */
class FakeProfileRepository(initial: Profile? = testProfile()) : ProfileRepository {
    val profile = MutableStateFlow(initial)
    var refreshCalls = 0
    var updateNameResult: AppResult<Unit> = AppResult.Success(Unit)
    var updatePhotoResult: AppResult<Unit> = AppResult.Success(Unit)
    val names = mutableListOf<String>()
    val photos = mutableListOf<PickedFile>()

    override fun observeProfile(): Flow<Profile?> = profile

    override suspend fun refresh(): AppResult<Unit> {
        refreshCalls++
        return AppResult.Success(Unit)
    }

    override suspend fun updateName(name: String): AppResult<Unit> {
        names += name
        if (updateNameResult is AppResult.Success) profile.value = profile.value?.copy(name = name)
        return updateNameResult
    }

    override suspend fun updatePhoto(file: PickedFile): AppResult<Unit> {
        photos += file
        if (updatePhotoResult is AppResult.Success) profile.value = profile.value?.copy(photoUrl = "http://13.222.72.73/fotos/${file.name}")
        return updatePhotoResult
    }
}
