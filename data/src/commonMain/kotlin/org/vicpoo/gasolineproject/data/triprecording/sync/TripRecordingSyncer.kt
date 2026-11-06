package org.vicpoo.gasolineproject.data.triprecording.sync

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.httpCode
import org.vicpoo.gasolineproject.domain.common.serverMessage
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.data.network.safeApiCall
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.domain.sync.PendingSyncKind
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.domain.sync.SyncRejection
import org.vicpoo.gasolineproject.domain.sync.stopsSync
import org.vicpoo.gasolineproject.data.triprecording.local.FuelEntity
import org.vicpoo.gasolineproject.data.triprecording.local.FuelSyncState
import org.vicpoo.gasolineproject.data.triprecording.local.PauseEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TrackPointEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.data.triprecording.remote.CargaFormData
import org.vicpoo.gasolineproject.data.triprecording.remote.FinalizarViajeRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.IniciarViajeRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.PausaSincronizadaDto
import org.vicpoo.gasolineproject.data.triprecording.remote.PausarViajeRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.ReanudarViajeRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.PuntoRutaRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.ReceiptUpload
import org.vicpoo.gasolineproject.data.triprecording.remote.SincronizarViajeRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.TripRecordingApi
import org.vicpoo.gasolineproject.domain.triprecording.SyncLogResult
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import kotlin.math.round

/**
 * Envía a APIGasoline lo que se registró en el teléfono, en este orden por viaje:
 *  1. El viaje: POST /viajes (si sigue en curso) o POST /viajes/sincronizar (si se hizo completo sin red).
 *  2. Las recargas: POST / PUT / DELETE /cargas-combustible (multipart con la foto del recibo).
 *     Van ANTES que los puntos GPS: son lo más importante y lo más chico. Antes iban después de los
 *     puntos y con señal débil un viaje largo (miles de puntos) nunca dejaba llegar a las recargas.
 *  3. Las pausas: POST /viajes/{id}/pausar y /reanudar (en orden y con la hora del teléfono).
 *  4. Los puntos GPS en lotes de 100: POST /viajes/{id}/puntos-ruta (la API ignora duplicados).
 *     Antes del cierre, para que la API calcule la ruta una sola vez al finalizar.
 *  5. El cierre: POST /viajes/{id}/finalizar con el kilometraje que capturó el usuario.
 *  6. La foto del odómetro: PUT /viajes/{id}/foto-tablero/final.
 *  7. Si todo llegó, borra el viaje del teléfono.
 *
 * Un viaje completo hecho sin red (ya guardado en "Registrar Viaje") se manda de una vez con
 * POST /viajes/sincronizar, incluidas sus pausas.
 *
 * Sin red o con sesión vencida se detiene, lo deja pendiente y pide al [BackgroundSyncScheduler]
 * que lo vuelva a intentar en cuanto haya internet (aunque la app esté cerrada).
 *
 * Si la API rechaza algo guarda el mensaje y el código en ese registro y continúa con lo demás.
 * Un rechazo definitivo (4xx, ver [SyncRejection]) ya no se reenvía en automático: espera a que el
 * usuario lo corrija (editar la recarga / volver a guardar "Registrar Viaje") o toque "Reintentar".
 * Un lote de puntos GPS rechazado se descarta (se cuenta en pointsDiscarded) para no bloquear el viaje.
 *
 * Cada paso queda en la bitácora [log] (pantalla "Envíos pendientes") con su resultado y duración.
 */
class TripRecordingSyncer(
    private val store: TripRecordingLocalStore,
    private val api: TripRecordingApi,
    private val appScope: AppScope,
    private val scheduler: BackgroundSyncScheduler,
    private val log: TripSyncLog? = null,
) : TripRecordingSync, PendingSyncTask {

    private val mutex = Mutex()

    override val kind = PendingSyncKind.TRIPS

    private val syncing = MutableStateFlow(false)

    /** true mientras hay un envío en curso (lo muestra la pantalla de diagnóstico). */
    val isSyncing: StateFlow<Boolean> = syncing.asStateFlow()

    // Cada paso vive en su propia clase (una responsabilidad cada una)
    private val trips = TripUploader(store, api)
    private val points = RoutePointsUploader(store, api)
    private val pauses = PauseUploader(store, api)
    private val fuelLoads = FuelLoadUploader(store, api)

    override fun requestSync() {
        appScope.scope.launch { sync() }
    }

    /**
     * El envío corre en el scope de la APP (no en el de la pantalla): si el usuario cierra el
     * formulario o la app pasa a segundo plano, sigue. Aquí solo se espera un rato el resultado.
     */
    override suspend fun syncWithin(timeoutMillis: Long): AppResult<Unit>? {
        val running = appScope.scope.async { sync() }
        return withTimeoutOrNull(timeoutMillis) { running.await() }
    }

    override suspend fun syncPending(): AppResult<Unit> = sync()

    /**
     * Un viaje se borra del teléfono en cuanto todo llegó a la API, así que cada viaje que quede
     * (en curso, sin enviar o rechazado) está pendiente, igual que las recargas sin sincronizar.
     */
    override suspend fun pendingCount(): Int {
        val snapshot = store.current()
        return snapshot.trips.size +
            snapshot.fuel.count { it.state != FuelSyncState.SYNCED || it.syncErrorMessage != null }
    }

    override suspend fun sync(): AppResult<Unit> = mutex.withLock {
        syncing.value = true
        try {
            for (trip in store.current().trips) {
                val result = syncTrip(trip.localId)
                if (result is AppResult.Failure) {
                    if (result.error == AppError.NoConnection) scheduler.requestWhenOnline()
                    return@withLock result
                }
            }
            AppResult.Success(Unit)
        } finally {
            syncing.value = false
        }
    }

    /** Failure solo cuando hay que detener todo (sin red / sesión vencida). */
    private suspend fun syncTrip(localId: String): AppResult<Unit> {
        // 1. Viaje
        var trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (trip.serverId == null) {
            // Rechazado de forma definitiva: espera a que el usuario lo corrija.
            // Excepción: si el viaje sigue en curso se reintenta en cada sincronización (cada minuto,
            // al pausar/reanudar o al guardar una recarga). El rechazo suele depender de algo que se
            // arregla en el servidor (ej. "el carro ya tiene un viaje en curso") y el operador no
            // tiene forma de corregirlo desde la app.
            if (trip.isBlocked() && !trip.running) {
                flagFuelBlockedByTrip(localId, trip.syncErrorMessage)
                return AppResult.Success(Unit)
            }
            when (val created = tripStep(localId, STEP_TRIP_CREATE) { trips.create(trip) }) {
                is Step.Stop -> return AppResult.Failure(created.error)
                is Step.Rejected -> {
                    // El error quedó guardado en el viaje; las recargas lo muestran para que se sepa por qué no suben
                    flagFuelBlockedByTrip(localId, currentTrip(localId)?.syncErrorMessage)
                    return AppResult.Success(Unit)
                }
                // Error temporal: el viaje se vuelve a intentar en la siguiente sincronización
                is Step.Deferred -> return AppResult.Success(Unit)
                is Step.Done -> trip = currentTrip(localId) ?: return AppResult.Success(Unit)
            }
        }
        val serverId = trip.serverId ?: return AppResult.Success(Unit)

        // 2. Recargas (lo más importante primero)
        val fuel = store.current().fuel.filter {
            it.tripLocalId == localId && it.state != FuelSyncState.SYNCED && !SyncRejection.isPermanent(it.syncErrorCode)
        }
        for (entity in fuel) {
            (fuelStep(entity) { fuelLoads.sync(entity, serverId) } as? Step.Stop)?.let { return AppResult.Failure(it.error) }
        }

        // 3. Pausas (solo mientras la API tiene el viaje en curso)
        trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (!trip.closedOnServer && trip.hasPendingPauses()) {
            (tripStep(localId, STEP_PAUSES) { pauses.send(trip, serverId) } as? Step.Stop)?.let { return AppResult.Failure(it.error) }
        }

        // 4. Puntos GPS
        trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        val pendingPoints = store.countPoints(localId) - trip.pointsSent
        if (pendingPoints > 0) {
            val step = "$STEP_POINTS ($pendingPoints pendientes)"
            (tripStep(localId, step) { points.send(trip, serverId) } as? Step.Stop)?.let { return AppResult.Failure(it.error) }
        }

        // 5. Cierre (solo cuando el usuario ya guardó "Registrar Viaje" y llegaron TODAS las pausas:
        //    con el viaje cerrado la API ya no acepta pausas y se perderían)
        trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (!trip.running && !trip.closedOnServer && !trip.isBlocked() && !trip.hasPendingPauses()) {
            (tripStep(localId, STEP_TRIP_FINISH) { trips.finish(trip, serverId) } as? Step.Stop)
                ?.let { return AppResult.Failure(it.error) }
        }

        // 6. Foto del odómetro
        trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (trip.closedOnServer && trip.odometerPhotoPath != null && !trip.odometerPhotoSent) {
            (tripStep(localId, STEP_ODOMETER_PHOTO) { trips.uploadOdometerPhoto(trip, serverId) } as? Step.Stop)
                ?.let { return AppResult.Failure(it.error) }
        }

        // 7. Limpieza
        cleanupIfDone(localId)
        return AppResult.Success(Unit)
    }

    /** 7. Si todo llegó a la API, se borra el viaje del teléfono (puntos, fotos y recargas). */
    private suspend fun cleanupIfDone(localId: String) {
        val snapshot = store.current()
        val trip = snapshot.trips.firstOrNull { it.localId == localId } ?: return
        if (trip.running || !trip.closedOnServer || trip.syncErrorMessage != null) return
        if (trip.odometerPhotoPath != null && !trip.odometerPhotoSent) return
        val fuelPending = snapshot.fuel.any { it.tripLocalId == localId && (it.state != FuelSyncState.SYNCED || it.syncErrorMessage != null) }
        if (fuelPending) return
        if (store.countPoints(localId) > trip.pointsSent) return
        store.deleteFile(trip.odometerPhotoPath)
        store.removeTrip(localId)
        log?.record(STEP_TRIP_DONE, SyncLogResult.OK)
    }

    // ---------------- Bitácora ----------------

    /** Ejecuta un paso del viaje y lo anota en la bitácora (con el error que haya quedado en el viaje). */
    private suspend fun tripStep(localId: String, step: String, block: suspend () -> Step): Step {
        val startedAt = log?.nowMillis()
        val result = block()
        log?.let { log ->
            val trip = currentTrip(localId)
            log.recordStep(step, result, trip?.syncErrorMessage, trip?.syncErrorCode, startedAt)
        }
        return result
    }

    /** Ejecuta el envío de una recarga y lo anota en la bitácora. */
    private suspend fun fuelStep(entity: FuelEntity, block: suspend () -> Step): Step {
        val startedAt = log?.nowMillis()
        val result = block()
        log?.let { log ->
            val action = when {
                entity.state == FuelSyncState.PENDING_DELETE -> "borrar"
                entity.serverId == null -> "enviar"
                else -> "corregir"
            }
            val saved = store.current().fuel.firstOrNull { it.localId == entity.localId }
            log.recordStep("Recarga ${entity.number}#: $action", result, saved?.syncErrorMessage, saved?.syncErrorCode, startedAt)
        }
        return result
    }

    private suspend fun TripSyncLog.recordStep(step: String, result: Step, message: String?, code: Int?, startedAt: Long?) {
        when (result) {
            Step.Done -> record(step, SyncLogResult.OK, startedAtMillis = startedAt)
            Step.Rejected -> record(step, SyncLogResult.REJECTED, message, code, startedAt)
            is Step.Deferred -> record(step, SyncLogResult.ERROR, TEMPORARY_ERROR_PREFIX + result.error.messageOrDefault(), result.error.httpCode(), startedAt)
            is Step.Stop -> when (result.error) {
                AppError.NoConnection -> record(step, SyncLogResult.OFFLINE, NO_CONNECTION_MESSAGE, null, startedAt)
                AppError.Unauthorized -> record(step, SyncLogResult.ERROR, SESSION_ENDED_MESSAGE, 401, startedAt)
                else -> record(step, SyncLogResult.ERROR, result.error.messageOrDefault(), result.error.httpCode(), startedAt)
            }
        }
    }

    // ---------------- Utilidades ----------------

    /**
     * La API no aceptó el viaje, así que sus recargas no se pueden mandar todavía.
     * Se les pone el motivo del viaje (sin código: no es un rechazo definitivo de la recarga) para que
     * el usuario vea el error real en lugar de un "error en el servidor" genérico.
     * Al tocar "Reintentar" en la recarga se limpia este mensaje y también el bloqueo del viaje.
     */
    private suspend fun flagFuelBlockedByTrip(localId: String, tripMessage: String?) {
        val message = TRIP_NOT_REGISTERED + (tripMessage ?: DEFAULT_TRIP_REJECTED)
        val pending = store.current().fuel.filter {
            it.tripLocalId == localId && it.state != FuelSyncState.SYNCED && it.syncErrorCode == null
        }
        for (entity in pending) {
            store.updateFuel(entity.localId) { it.copy(syncErrorMessage = message, syncErrorCode = null) }
        }
    }

    private fun TripEntity.hasPendingPauses(): Boolean = pauses.any { !it.startSent || (!it.isOpen && !it.endSent) }

    private companion object {
        const val TRIP_NOT_REGISTERED = "El viaje no se pudo registrar en el servidor: "
        const val DEFAULT_TRIP_REJECTED = "el servidor lo rechazó."

        const val STEP_TRIP_CREATE = "Viaje: registrar en el servidor"
        const val STEP_PAUSES = "Pausas del viaje"
        const val STEP_POINTS = "Ruta GPS"
        const val STEP_TRIP_FINISH = "Viaje: terminar"
        const val STEP_ODOMETER_PHOTO = "Foto del odómetro"
        const val STEP_TRIP_DONE = "Viaje completo en el servidor (se borró del teléfono)"

        const val TEMPORARY_ERROR_PREFIX = "Error temporal del servidor (se reintenta solo): "
        const val NO_CONNECTION_MESSAGE = "Sin conexión o señal débil (no respondió a tiempo). Se reintenta solo."
        const val SESSION_ENDED_MESSAGE = "La sesión terminó. Inicia sesión para continuar el envío."
    }

    private suspend fun currentTrip(localId: String): TripEntity? =
        store.current().trips.firstOrNull { it.localId == localId }
}
