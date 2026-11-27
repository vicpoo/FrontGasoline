package org.vicpoo.gasolineproject.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.location.LocationFix
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.sync.PendingSyncKind
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicle
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicleRepository
import org.vicpoo.gasolineproject.domain.triprecording.CostCenter
import org.vicpoo.gasolineproject.domain.triprecording.CostCenterRepository
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecord
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordRepository
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordValues
import org.vicpoo.gasolineproject.domain.triprecording.ReceiptPhoto
import org.vicpoo.gasolineproject.domain.triprecording.RecordSyncState
import org.vicpoo.gasolineproject.domain.triprecording.RecordedTrip
import org.vicpoo.gasolineproject.domain.triprecording.ReleaseBlock
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome
import org.vicpoo.gasolineproject.domain.triprecording.TrackingSession
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

/*
 * Fakes del registro de viajes (carros asignados, centros de costo, tracker GPS, recargas y
 * sincronización). Todo en memoria: la prueba cambia el estado y revisa qué se llamó.
 */

/** Carro asignado de prueba (kilometraje 1,000 y tanque de 50 L). */
fun testAssignedVehicle(
    reservationId: String = "res-1",
    mileage: Double = 1_000.0,
    tankCapacityLiters: Double? = 50.0,
) = AssignedVehicle(
    reservationId = reservationId,
    vehicleId = "car-$reservationId",
    brand = "Nissan",
    model = "Versa",
    plates = "ABC-123",
    color = "Blanco",
    photoUrl = null,
    mileage = mileage,
    tankCapacityLiters = tankCapacityLiters,
)

fun testCostCenter(id: String = "cc-1", name: String = "Ventas") = CostCenter(id = id, name = name, key = id.uppercase())

/** Viaje de prueba en la fase indicada. */
fun testRecordedTrip(
    phase: TripPhase = TripPhase.RUNNING,
    vehicle: AssignedVehicle = testAssignedVehicle(),
    costCenter: CostCenter? = null,
    finalMileage: Double? = null,
) = RecordedTrip(
    localId = "trip-1",
    serverId = null,
    vehicle = vehicle,
    costCenter = costCenter,
    startedAt = "2026-11-23T08:00:00-06:00",
    startedAtMillis = 0L,
    endedAt = null,
    endedAtMillis = null,
    distanceMeters = 12_500.0,
    lastLocation = GeoPoint(16.75, -93.12),
    isRunning = phase == TripPhase.RUNNING,
    syncState = RecordSyncState.PENDING,
    syncErrorMessage = null,
    phase = phase,
    finalMileage = finalMileage,
)

/** Reloj fijo: la prueba cambia [now] para simular el paso del tiempo. */
class FakeAppClock(var now: Long = 0L) : AppClock {
    override fun nowMillis(): Long = now
    override fun toLocalIso(epochMillis: Long): String = "2026-11-23T08:00:00-06:00"
}

/** Ubicación falsa: con o sin permiso y con una última ubicación opcional. */
class FakeLocationProvider(
    var permission: Boolean = true,
    var lastKnownFix: LocationFix? = null,
) : LocationProvider {
    override fun hasPermission(): Boolean = permission
    override fun isLocationEnabled(): Boolean = true
    override fun updates(intervalMillis: Long, minDistanceMeters: Float): Flow<LocationFix> = emptyFlow()
    override suspend fun lastKnown(): LocationFix? = lastKnownFix
}

/**
 * TripTracker en memoria: cambia la fase del viaje como el real, sin GPS.
 * - [failure]: si no es null, todas las acciones del viaje fallan con ese error.
 * - [finishResult] / [retryOutcome]: lo que regresa "Registrar Viaje" y "Reintentar".
 */
class FakeTripTracker(initial: TrackingSession = TrackingSession()) : TripTracker {
    val state = MutableStateFlow(initial)
    override val session: StateFlow<TrackingSession> = state

    val calls = mutableListOf<String>()
    var failure: AppError? = null
    var finishResult: AppResult<SendOutcome> = AppResult.Success(SendOutcome.Sent)
    var retryOutcome: SendOutcome = SendOutcome.Sent
    var finishedWith: Pair<Double, PickedFile>? = null
    /** closedByLogout del último finish (cerrar el viaje para salir de la cuenta). */
    var finishedByLogout: Boolean? = null
    var startedWith: Pair<AssignedVehicle, CostCenter?>? = null
    var permissionChanges = 0

    override suspend fun start(vehicle: AssignedVehicle, costCenter: CostCenter?): AppResult<RecordedTrip> {
        calls += "start"
        failure?.let { return AppResult.Failure(it) }
        startedWith = vehicle to costCenter
        val trip = testRecordedTrip(vehicle = vehicle, costCenter = costCenter)
        state.value = state.value.copy(trip = trip)
        return AppResult.Success(trip)
    }

    override suspend fun pause(): AppResult<RecordedTrip> = changePhase("pause", TripPhase.PAUSED)
    override suspend fun resume(): AppResult<RecordedTrip> = changePhase("resume", TripPhase.RUNNING)
    override suspend fun stop(): AppResult<RecordedTrip> = changePhase("stop", TripPhase.STOPPED)
    override suspend fun undoStop(): AppResult<RecordedTrip> = changePhase("undoStop", TripPhase.RUNNING)

    override suspend fun finish(finalMileage: Double, odometerPhoto: PickedFile, closedByLogout: Boolean): AppResult<SendOutcome> {
        calls += "finish"
        finishedWith = finalMileage to odometerPhoto
        finishedByLogout = closedByLogout
        return finishResult
    }

    override suspend fun retryFinishSend(): SendOutcome {
        calls += "retryFinishSend"
        return retryOutcome
    }

    override fun onPermissionChanged() {
        permissionChanges++
    }

    private fun changePhase(name: String, phase: TripPhase): AppResult<RecordedTrip> {
        calls += name
        failure?.let { return AppResult.Failure(it) }
        val trip = state.value.trip ?: return AppResult.Failure(AppError.NotFound)
        val updated = trip.copy(phase = phase, isRunning = phase == TripPhase.RUNNING)
        state.value = state.value.copy(trip = updated)
        return AppResult.Success(updated)
    }
}

/** Carros asignados en memoria. "Dejar carro" lo quita de la lista si [releaseResult] es éxito. */
class FakeAssignedVehicleRepository(initial: List<AssignedVehicle> = emptyList()) : AssignedVehicleRepository {
    val vehicles = MutableStateFlow(initial)
    val releaseBlocks = MutableStateFlow<Map<String, ReleaseBlock>>(emptyMap())
    var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
    var releaseResult: AppResult<Unit> = AppResult.Success(Unit)
    var refreshCalls = 0
    val released = mutableListOf<Pair<String, String?>>()

    override fun observe(): Flow<List<AssignedVehicle>> = vehicles

    override suspend fun refresh(): AppResult<Unit> {
        refreshCalls++
        return refreshResult
    }

    override suspend fun find(reservationId: String): AssignedVehicle? =
        vehicles.value.firstOrNull { it.reservationId == reservationId }

    override fun observeReleaseBlocks(): Flow<Map<String, ReleaseBlock>> = releaseBlocks

    override suspend fun release(reservationId: String, notes: String?): AppResult<Unit> {
        released += reservationId to notes
        if (releaseResult is AppResult.Success) {
            vehicles.value = vehicles.value.filterNot { it.reservationId == reservationId }
        }
        return releaseResult
    }
}

/** Centros de costo en memoria. */
class FakeCostCenterRepository(initial: List<CostCenter> = emptyList()) : CostCenterRepository {
    val centers = MutableStateFlow(initial)
    var refreshCalls = 0

    override fun observe(): Flow<List<CostCenter>> = centers

    override suspend fun refresh(): AppResult<Unit> {
        refreshCalls++
        return AppResult.Success(Unit)
    }
}

/**
 * Recargas en memoria. Al guardar, la recarga queda con [stateAfterSave] (por omisión SYNCED,
 * como si ya hubiera llegado al servidor).
 */
class FakeFuelRecordRepository(initial: List<FuelRecord> = emptyList()) : FuelRecordRepository {
    val records = MutableStateFlow(initial)
    var stateAfterSave: RecordSyncState = RecordSyncState.SYNCED
    var errorMessageAfterSave: String? = null
    val savedReceipts = mutableListOf<PickedFile?>()
    val deleted = mutableListOf<String>()
    val retried = mutableListOf<String>()

    override fun observeForTrip(tripLocalId: String): Flow<List<FuelRecord>> =
        records.map { list -> list.filter { it.tripLocalId == tripLocalId } }

    override suspend fun get(localId: String): FuelRecord? = records.value.firstOrNull { it.localId == localId }

    override suspend fun save(
        tripLocalId: String,
        localId: String?,
        values: FuelRecordValues,
        newReceipt: PickedFile?,
        location: GeoPoint?,
    ): AppResult<FuelRecord> {
        savedReceipts += newReceipt
        val existing = localId?.let { get(it) }
        val record = FuelRecord(
            localId = localId ?: "fuel-${records.value.size + 1}",
            serverId = existing?.serverId,
            tripLocalId = tripLocalId,
            number = existing?.number ?: (records.value.size + 1),
            amount = values.amount,
            liters = values.liters,
            costCenter = values.costCenter,
            receipt = newReceipt?.let { ReceiptPhoto.Local("/fotos/${it.name}") } ?: existing?.receipt,
            loadedAt = "2026-11-23T10:00:00-06:00",
            location = location,
            syncState = stateAfterSave,
            syncErrorMessage = errorMessageAfterSave,
        )
        records.value = records.value.filterNot { it.localId == record.localId } + record
        return AppResult.Success(record)
    }

    override suspend fun delete(localId: String): AppResult<Unit> {
        deleted += localId
        records.value = records.value.filterNot { it.localId == localId }
        return AppResult.Success(Unit)
    }

    override suspend fun markForRetry(localId: String) {
        retried += localId
        records.value = records.value.map {
            if (it.localId == localId) it.copy(syncState = RecordSyncState.PENDING, syncErrorMessage = null) else it
        }
    }
}

/** Recarga de prueba ya guardada. */
fun testFuelRecord(
    localId: String = "fuel-1",
    tripLocalId: String = "trip-1",
    costCenter: CostCenter? = null,
    state: RecordSyncState = RecordSyncState.SYNCED,
) = FuelRecord(
    localId = localId,
    serverId = null,
    tripLocalId = tripLocalId,
    number = 1,
    amount = 500.0,
    liters = 20.0,
    costCenter = costCenter,
    receipt = ReceiptPhoto.Remote("http://13.222.72.73/fotos/recibo.jpg"),
    loadedAt = "2026-11-23T10:00:00-06:00",
    location = null,
    syncState = state,
    syncErrorMessage = null,
)

/**
 * Sincronización falsa del registro de viajes.
 * - [result]: lo que regresa (null en syncWithin = "sigue enviándose en segundo plano").
 * - [onSync]: lo que "hace" el envío (ej. marcar la recarga como enviada o rechazada).
 */
class FakeTripRecordingSync(var result: AppResult<Unit>? = AppResult.Success(Unit)) : TripRecordingSync {
    var onSync: () -> Unit = {}
    var syncCalls = 0
    var requestCalls = 0

    override suspend fun sync(): AppResult<Unit> {
        syncCalls++
        onSync()
        return result ?: AppResult.Success(Unit)
    }

    override fun requestSync() {
        requestCalls++
    }

    override suspend fun syncWithin(timeoutMillis: Long): AppResult<Unit>? {
        syncCalls++
        onSync()
        return result
    }
}

/** Tarea pendiente falsa (para el resumen de pendientes y "Enviar ahora"). */
class FakePendingSyncTask(
    override val kind: PendingSyncKind,
    var count: Int = 0,
    var result: AppResult<Unit> = AppResult.Success(Unit),
) : PendingSyncTask {
    var syncCalls = 0

    override suspend fun syncPending(): AppResult<Unit> {
        syncCalls++
        return result
    }

    override suspend fun pendingCount(): Int = count
}
