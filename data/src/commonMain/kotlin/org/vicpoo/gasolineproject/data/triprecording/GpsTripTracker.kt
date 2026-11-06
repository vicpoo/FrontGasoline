package org.vicpoo.gasolineproject.data.triprecording

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
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.location.LocationFix
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.session.SessionReader
import org.vicpoo.gasolineproject.data.triprecording.local.PauseEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TrackPointEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicle
import org.vicpoo.gasolineproject.domain.triprecording.CostCenter
import org.vicpoo.gasolineproject.domain.triprecording.RecordedTrip
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome
import org.vicpoo.gasolineproject.domain.triprecording.TrackingSession
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker
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

    /** Decide qué lecturas cuentan para la ruta y cuánta distancia suman. */
    private val routeFilter = RouteFilter()

    /** Traduce el resultado del envío de "Registrar Viaje" a lo que ve el usuario. */
    private val reporter = FinishSendReporter(store, sync)

    /** Último viaje que se guardó en "Registrar Viaje" (para "Reintentar"). */
    private var lastFinishedLocalId: String? = null

    init {
        // Si la app se cerró con un viaje en curso, se reanuda al abrirla
        appScope.scope.launch { restore() }
    }

    private suspend fun restore() = mutex.withLock {
        val active = store.current().activeTrip ?: return@withLock
        val route = store.readRoute(active.localId)
        routeFilter.reset(from = route.lastOrNull())
        state.value = TrackingSession(
            trip = active.toDomain(),
            route = route,
            current = active.lastPoint(),
            hasLocationPermission = locationProvider.hasPermission(),
            hasPreciseLocation = locationProvider.hasPreciseLocation(),
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

            routeFilter.reset()
            // Punto de partida: con una ubicación reciente y precisa, la distancia empieza a contar desde
            // aquí. Sin esto se perdía lo recorrido mientras el GPS daba su primera lectura buena (con el
            // GPS "frío" pueden ser varios cientos de metros). Se guarda como punto para que la API
            // calcule la misma distancia.
            val anchor = firstFix?.copy(timeMillis = now)?.takeIf { routeFilter.accept(it) != null }
            if (anchor != null) {
                store.appendPoint(
                    entity.localId,
                    TrackPointEntity(
                        lat = anchor.lat,
                        lng = anchor.lng,
                        t = clock.toLocalIso(now),
                        acc = anchor.accuracyMeters,
                        alt = anchor.altitudeMeters,
                        spd = null,
                    ),
                )
            }
            state.value = TrackingSession(
                trip = entity.toDomain(),
                route = listOfNotNull(anchor?.point),
                current = firstFix?.point,
                hasLocationPermission = locationProvider.hasPermission(),
                hasPreciseLocation = locationProvider.hasPreciseLocation(),
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
        routeFilter.reset()
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
            routeFilter.reset()
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
                    syncErrorCode = null,
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
                state.value = TrackingSession(
                    hasLocationPermission = locationProvider.hasPermission(),
                    hasPreciseLocation = locationProvider.hasPreciseLocation(),
                )
            }
        }
        return AppResult.Success(outcome)
    }

    override suspend fun retryFinishSend(): SendOutcome {
        val localId = lastFinishedLocalId ?: return SendOutcome.Sent
        // El usuario pidió reintentar: se quita el rechazo para que la sincronización lo vuelva a enviar
        store.updateTrip(localId) { it.copy(syncErrorMessage = null, syncErrorCode = null) }
        return sendAndReport(localId)
    }

    /** Sincroniza y traduce lo que pasó con el viaje a un [SendOutcome]. */
    private suspend fun sendAndReport(localId: String): SendOutcome = reporter.report(localId)

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
                state.update {
                    it.copy(
                        hasLocationPermission = locationProvider.hasPermission(),
                        hasPreciseLocation = locationProvider.hasPreciseLocation(),
                    )
                }
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
            var signalLost = false
            while (isActive) {
                delay(HEARTBEAT_MS)
                val active = clock.nowMillis() - lastFixMillis < GPS_TIMEOUT_MS
                state.update {
                    it.copy(
                        gpsActive = active,
                        hasLocationPermission = locationProvider.hasPermission(),
                        hasPreciseLocation = locationProvider.hasPreciseLocation(),
                    )
                }
                // Más de GPS_TIMEOUT_MS sin lecturas: se avisa en la notificación (y se quita al volver)
                if (signalLost == active) {
                    signalLost = !active
                    trackingService.setGpsSignalLost(signalLost)
                }
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

        // Lecturas imprecisas, ruido estando detenido o saltos imposibles no cuentan (ver RouteFilter)
        val added = routeFilter.accept(fix) ?: return@withLock

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
        /**
         * Una lectura por segundo y sin distancia mínima (como Strava): con 5 s / 10 m la ruta "cortaba"
         * las curvas y medía de menos. El ruido estando detenido lo limpia RouteFilter.
         */
        const val GPS_INTERVAL_MS = 1_000L
        const val MIN_DISTANCE_M = 0f
        const val HEARTBEAT_MS = 5_000L
        const val GPS_TIMEOUT_MS = 30_000L
        const val SYNC_EVERY_TICKS = 12 // 12 x 5 s = cada minuto
        const val STALE_FIX_MS = 2 * 60_000L
    }
}
