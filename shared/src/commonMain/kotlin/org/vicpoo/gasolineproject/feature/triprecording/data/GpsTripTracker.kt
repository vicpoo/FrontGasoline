package org.vicpoo.gasolineproject.feature.triprecording.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.core.common.AppClock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.GeoMath
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.di.AppScope
import org.vicpoo.gasolineproject.core.location.LocationFix
import org.vicpoo.gasolineproject.core.location.LocationProvider
import org.vicpoo.gasolineproject.core.location.TrackingServiceController
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.feature.triprecording.data.local.PauseEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TrackPointEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicle
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenter
import org.vicpoo.gasolineproject.feature.triprecording.domain.RecordedTrip
import org.vicpoo.gasolineproject.feature.triprecording.domain.SendOutcome
import org.vicpoo.gasolineproject.feature.triprecording.domain.TrackingSession
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripPhase
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripRecordingSync
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripTracker
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Registra el viaje con el GPS:
 * - Guarda cada punto en el teléfono (no se pierde si se va la señal o se cierra la app).
 * - Calcula la distancia descartando lecturas imprecisas y saltos imposibles.
 * - Cada minuto intenta mandar lo pendiente a la API.
 * - En pausa y con el viaje detenido (esperando kilometraje y foto) el GPS está apagado.
 * Vive en el AppScope (no en una pantalla), y el servicio en primer plano lo mantiene vivo
 * con la pantalla apagada.
 */
class GpsTripTracker(
    private val store: TripRecordingLocalStore,
    private val locationProvider: LocationProvider,
    private val trackingService: TrackingServiceController,
    private val sync: TripRecordingSync,
    private val sessionReader: SessionReader,
    private val clock: AppClock,
    private val appScope: AppScope,
) : TripTracker {

    private val state = MutableStateFlow(TrackingSession())
    override val session: StateFlow<TrackingSession> = state.asStateFlow()

    private val mutex = Mutex()
    private var gpsJob: Job? = null
    private var heartbeatJob: Job? = null
    private var lastFixMillis: Long = 0L
    private var lastRoutePoint: GeoPoint? = null
    private var lastRoutePointMillis: Long = 0L

    /** Último viaje que se guardó en "Registrar Viaje" (para "Reintentar"). */
    private var lastFinishedLocalId: String? = null

    init {
        // Si la app se cerró con un viaje en curso, se reanuda al abrirla
        appScope.scope.launch { restore() }
    }

    private suspend fun restore() = mutex.withLock {
        val active = store.current().activeTrip ?: return@withLock
        val route = store.readRoute(active.localId)
        lastRoutePoint = route.lastOrNull()
        state.value = TrackingSession(
            trip = active.toDomain(),
            route = route,
            current = active.lastPoint(),
            hasLocationPermission = locationProvider.hasPermission(),
        )
        // En pausa o detenido no se enciende el GPS
        if (active.phase() == TripPhase.RUNNING) startTracking()
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun start(vehicle: AssignedVehicle, costCenter: CostCenter?): AppResult<RecordedTrip> =
        mutex.withLock {
            state.value.trip?.let { return@withLock AppResult.Success(it) }

            val session = sessionReader.current()
            val employeeId = session?.employeeId
            val companyId = session?.companyId
            if (employeeId == null || companyId == null) return@withLock AppResult.Failure(AppError.NoCompany)

            val firstFix = locationProvider.lastKnown()?.takeIf { clock.nowMillis() - it.timeMillis < STALE_FIX_MS }
            val now = clock.nowMillis()
            val entity = TripEntity(
                localId = Uuid.random().toString(),
                companyId = companyId,
                employeeId = employeeId,
                vehicle = vehicle.toEntity(),
                costCenter = costCenter?.toEntity(),
                startedAt = clock.toLocalIso(now),
                startedAtMillis = now,
                startLat = firstFix?.lat,
                startLng = firstFix?.lng,
                initialMileage = vehicle.mileage,
                lastLat = firstFix?.lat,
                lastLng = firstFix?.lng,
                running = true,
            )
            store.update { it.copy(trips = it.trips + entity) }

            lastRoutePoint = null
            lastRoutePointMillis = 0L
            state.value = TrackingSession(
                trip = entity.toDomain(),
                route = emptyList(),
                current = firstFix?.point,
                hasLocationPermission = locationProvider.hasPermission(),
            )
            startTracking()
            sync.requestSync()
            AppResult.Success(entity.toDomain())
        }

    // ---------------- Pausa / reanudar ----------------

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun pause(): AppResult<RecordedTrip> = mutex.withLock {
        val trip = state.value.trip ?: return@withLock AppResult.Failure(AppError.NotFound)
        if (trip.phase != TripPhase.RUNNING) return@withLock AppResult.Success(trip)

        stopTracking()
        val now = clock.nowMillis()
        val updated = store.updateTrip(trip.localId) { entity ->
            entity.copy(
                pauses = entity.pauses + PauseEntity(
                    localId = Uuid.random().toString(),
                    startedAt = clock.toLocalIso(now),
                    startedAtMillis = now,
                    lat = entity.lastLat,
                    lng = entity.lastLng,
                ),
            )
        } ?: return@withLock AppResult.Failure(AppError.NotFound)

        state.update { it.copy(trip = updated.toDomain(), gpsActive = false) }
        sync.requestSync()
        AppResult.Success(updated.toDomain())
    }

    override suspend fun resume(): AppResult<RecordedTrip> = mutex.withLock {
        val trip = state.value.trip ?: return@withLock AppResult.Failure(AppError.NotFound)
        if (trip.phase != TripPhase.PAUSED) return@withLock AppResult.Success(trip)

        val updated = closeOpenPause(trip.localId, clock.nowMillis())
            ?: return@withLock AppResult.Failure(AppError.NotFound)

        // Lo que se movió durante la pausa no se suma a la distancia
        lastRoutePoint = null
        lastRoutePointMillis = 0L
        state.update { it.copy(trip = updated.toDomain()) }
        startTracking()
        sync.requestSync()
        AppResult.Success(updated.toDomain())
    }

    // ---------------- Terminar ----------------

    override suspend fun stop(): AppResult<RecordedTrip> = mutex.withLock {
        val trip = state.value.trip ?: return@withLock AppResult.Failure(AppError.NotFound)
        if (trip.phase == TripPhase.STOPPED) return@withLock AppResult.Success(trip)

        stopTracking()
        val now = clock.nowMillis()
        val updated = store.updateTrip(trip.localId) { entity ->
            entity.copy(
                endedAt = clock.toLocalIso(now),
                endedAtMillis = now,
                endLat = entity.lastLat,
                endLng = entity.lastLng,
            )
        } ?: return@withLock AppResult.Failure(AppError.NotFound)

        state.update { it.copy(trip = updated.toDomain(), gpsActive = false) }
        AppResult.Success(updated.toDomain())
    }

    override suspend fun undoStop(): AppResult<RecordedTrip> = mutex.withLock {
        val trip = state.value.trip ?: return@withLock AppResult.Failure(AppError.NotFound)
        if (trip.phase != TripPhase.STOPPED) return@withLock AppResult.Success(trip)

        val updated = store.updateTrip(trip.localId) { entity ->
            entity.copy(endedAt = null, endedAtMillis = null, endLat = null, endLng = null)
        } ?: return@withLock AppResult.Failure(AppError.NotFound)

        state.update { it.copy(trip = updated.toDomain()) }
        if (updated.phase() == TripPhase.RUNNING) {
            lastRoutePoint = null
            lastRoutePointMillis = 0L
            startTracking()
        }
        AppResult.Success(updated.toDomain())
    }

    override suspend fun finish(finalMileage: Double, odometerPhoto: PickedFile): AppResult<SendOutcome> {
        val saved = mutex.withLock {
            val trip = state.value.trip ?: return AppResult.Failure(AppError.NotFound)
            if (trip.phase != TripPhase.STOPPED) return AppResult.Failure(AppError.NotFound)

            // La foto queda en el teléfono hasta que llegue a la API
            val extension = odometerPhoto.extension()
            val photoPath = store.saveReceipt(odometerPhoto.bytes, extension)
            var previousPhoto: String? = null
            val updated = store.updateTrip(trip.localId) { entity ->
                previousPhoto = entity.odometerPhotoPath
                entity.copy(
                    running = false,
                    finalMileage = finalMileage,
                    odometerPhotoPath = photoPath,
                    odometerPhotoMimeType = odometerPhoto.mimeType,
                    odometerPhotoSent = false,
                    syncErrorMessage = null,
                )
            } ?: return AppResult.Failure(AppError.NotFound)
            if (previousPhoto != photoPath) store.deleteFile(previousPhoto)

            lastFinishedLocalId = updated.localId
            updated
        }

        val outcome = sendAndReport(saved.localId)

        mutex.withLock {
            if (outcome is SendOutcome.Rejected) {
                // El servidor no lo aceptó (ej. kilometraje menor al registrado): vuelve a "Registrar Viaje"
                val reopened = store.updateTrip(saved.localId) { it.copy(running = true) }
                if (reopened != null) state.update { it.copy(trip = reopened.toDomain()) }
            } else {
                // Guardado (enviado o pendiente): ya no hay viaje en curso
                state.value = TrackingSession(hasLocationPermission = locationProvider.hasPermission())
            }
        }
        return AppResult.Success(outcome)
    }

    override suspend fun retryFinishSend(): SendOutcome {
        val localId = lastFinishedLocalId ?: return SendOutcome.Sent
        return sendAndReport(localId)
    }

    /** Sincroniza y traduce lo que pasó con el viaje a un [SendOutcome]. */
    private suspend fun sendAndReport(localId: String): SendOutcome {
        val result = sync.sync()
        val trip = store.current().trips.firstOrNull { it.localId == localId }
        return when {
            trip == null -> SendOutcome.Sent // ya se envió todo y se borró del teléfono
            trip.syncErrorMessage != null -> SendOutcome.Rejected(trip.syncErrorMessage)
            trip.closedOnServer -> SendOutcome.Sent // falta solo la foto: se sube sola después
            result is AppResult.Failure && result.error != AppError.NoConnection -> SendOutcome.Rejected(null)
            else -> SendOutcome.SavedOffline
        }
    }

    private suspend fun closeOpenPause(tripLocalId: String, nowMillis: Long): TripEntity? =
        store.updateTrip(tripLocalId) { entity ->
            entity.copy(
                pauses = entity.pauses.map { pause ->
                    if (pause.isOpen) pause.copy(endedAt = clock.toLocalIso(nowMillis), endedAtMillis = nowMillis) else pause
                },
            )
        }

    private fun PickedFile.extension(): String = when (mimeType.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }

    override fun onPermissionChanged() {
        appScope.scope.launch {
            mutex.withLock {
                state.update { it.copy(hasLocationPermission = locationProvider.hasPermission()) }
                if (state.value.trip?.phase == TripPhase.RUNNING && gpsJob?.isActive != true) startTracking()
            }
        }
    }

    // ---------------- GPS ----------------

    private fun startTracking() {
        trackingService.start()
        gpsJob?.cancel()
        gpsJob = appScope.scope.launch {
            locationProvider.updates(GPS_INTERVAL_MS, MIN_DISTANCE_M).collect { fix -> onFix(fix) }
        }
        heartbeatJob?.cancel()
        heartbeatJob = appScope.scope.launch {
            var ticks = 0
            while (isActive) {
                delay(HEARTBEAT_MS)
                val active = clock.nowMillis() - lastFixMillis < GPS_TIMEOUT_MS
                state.update { it.copy(gpsActive = active, hasLocationPermission = locationProvider.hasPermission()) }
                ticks++
                if (ticks % SYNC_EVERY_TICKS == 0) sync.requestSync()
            }
        }
    }

    private fun stopTracking() {
        gpsJob?.cancel()
        heartbeatJob?.cancel()
        gpsJob = null
        heartbeatJob = null
        trackingService.stop()
    }

    private suspend fun onFix(fix: LocationFix) = mutex.withLock {
        val trip = state.value.trip ?: return@withLock
        if (trip.phase != TripPhase.RUNNING) return@withLock // en pausa o detenido no se registra
        lastFixMillis = clock.nowMillis()
        state.update { it.copy(current = fix.point, gpsActive = true) }

        // Lecturas con mucho error no cuentan para la ruta ni la distancia
        val accurate = (fix.accuracyMeters ?: 0.0) <= MAX_ACCURACY_M
        if (!accurate) return@withLock

        val previous = lastRoutePoint
        var added = 0.0
        if (previous != null) {
            val meters = GeoMath.distanceMeters(previous, fix.point)
            if (meters < MIN_STEP_M) return@withLock // ruido estando detenido
            val seconds = ((fix.timeMillis - lastRoutePointMillis).coerceAtLeast(1L)) / 1000.0
            if (meters / seconds > MAX_SPEED_MPS) return@withLock // salto imposible del GPS
            added = meters
        }

        lastRoutePoint = fix.point
        lastRoutePointMillis = fix.timeMillis
        store.appendPoint(
            trip.localId,
            TrackPointEntity(
                lat = fix.lat,
                lng = fix.lng,
                t = clock.toLocalIso(fix.timeMillis),
                acc = fix.accuracyMeters,
                alt = fix.altitudeMeters,
                spd = fix.speedMetersPerSecond,
            ),
        )
        val updated = store.updateTrip(trip.localId) { entity ->
            entity.copy(
                distanceMeters = entity.distanceMeters + added,
                lastLat = fix.lat,
                lastLng = fix.lng,
                startLat = entity.startLat ?: fix.lat,
                startLng = entity.startLng ?: fix.lng,
            )
        } ?: return@withLock
        state.update { it.copy(trip = updated.toDomain(), route = it.route + fix.point) }
    }

    private fun TripEntity.lastPoint(): GeoPoint? =
        if (lastLat != null && lastLng != null) GeoPoint(lastLat, lastLng) else null

    /** Al cerrar sesión: se detiene el GPS (los datos los borra el LogoutListener). */
    fun reset() {
        stopTracking()
        state.value = TrackingSession()
    }

    private companion object {
        const val GPS_INTERVAL_MS = 5_000L
        const val MIN_DISTANCE_M = 5f
        const val HEARTBEAT_MS = 5_000L
        const val GPS_TIMEOUT_MS = 30_000L
        const val SYNC_EVERY_TICKS = 12 // 12 x 5 s = cada minuto
        const val STALE_FIX_MS = 2 * 60_000L
        const val MAX_ACCURACY_M = 50.0 // igual que el filtro de la API
        const val MIN_STEP_M = 3.0
        const val MAX_SPEED_MPS = 70.0 // ~250 km/h
    }
}
