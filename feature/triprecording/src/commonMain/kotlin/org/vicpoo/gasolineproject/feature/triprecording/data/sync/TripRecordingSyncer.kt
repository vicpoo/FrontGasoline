package org.vicpoo.gasolineproject.feature.triprecording.data.sync

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.httpCode
import org.vicpoo.gasolineproject.core.common.serverMessage
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.core.sync.PendingSyncTask
import org.vicpoo.gasolineproject.core.sync.SyncRejection
import org.vicpoo.gasolineproject.core.sync.stopsSync
import org.vicpoo.gasolineproject.feature.triprecording.data.local.FuelEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.FuelSyncState
import org.vicpoo.gasolineproject.feature.triprecording.data.local.PauseEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TrackPointEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.CargaFormData
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.FinalizarViajeRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.IniciarViajeRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.PausaSincronizadaDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.PausarViajeRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.ReanudarViajeRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.PuntoRutaRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.ReceiptUpload
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.SincronizarViajeRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.TripRecordingApi
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripRecordingSync
import kotlin.math.round

/**
 * Envía a APIGasoline lo que se registró en el teléfono, en este orden por viaje:
 *  1. El viaje: POST /viajes (si sigue en curso) o POST /viajes/sincronizar (si se hizo completo sin red).
 *  2. Los puntos GPS en lotes: POST /viajes/{id}/puntos-ruta (la API ignora duplicados).
 *  3. Las pausas: POST /viajes/{id}/pausar y /reanudar (en orden y con la hora del teléfono).
 *  4. Las recargas: POST / PUT / DELETE /cargas-combustible (multipart).
 *  5. El cierre: POST /viajes/{id}/finalizar con el kilometraje que capturó el usuario (después de
 *     las recargas, porque con el viaje cerrado solo un supervisor puede corregirlas).
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
 */
class TripRecordingSyncer(
    private val store: TripRecordingLocalStore,
    private val api: TripRecordingApi,
    private val appScope: AppScope,
    private val scheduler: BackgroundSyncScheduler,
) : TripRecordingSync, PendingSyncTask {

    private val mutex = Mutex()

    // Cada paso vive en su propia clase (una responsabilidad cada una)
    private val trips = TripUploader(store, api)
    private val points = RoutePointsUploader(store, api)
    private val pauses = PauseUploader(store, api)
    private val fuelLoads = FuelLoadUploader(store, api)

    override fun requestSync() {
        appScope.scope.launch { sync() }
    }

    override suspend fun syncPending(): AppResult<Unit> = sync()

    override suspend fun sync(): AppResult<Unit> = mutex.withLock {
        for (trip in store.current().trips) {
            val result = syncTrip(trip.localId)
            if (result is AppResult.Failure) {
                if (result.error == AppError.NoConnection) scheduler.requestWhenOnline()
                return@withLock result
            }
        }
        AppResult.Success(Unit)
    }

    /** Failure solo cuando hay que detener todo (sin red / sesión vencida). */
    private suspend fun syncTrip(localId: String): AppResult<Unit> {
        // 1. Viaje
        var trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (trip.serverId == null) {
            // Rechazado de forma definitiva: espera a que el usuario lo corrija
            if (trip.isBlocked()) return AppResult.Success(Unit)
            when (val created = trips.create(trip)) {
                is Step.Stop -> return AppResult.Failure(created.error)
                is Step.Rejected -> return AppResult.Success(Unit) // el error quedó guardado en el viaje
                is Step.Done -> trip = currentTrip(localId) ?: return AppResult.Success(Unit)
            }
        }
        val serverId = trip.serverId ?: return AppResult.Success(Unit)

        // 2. Puntos GPS
        (points.send(trip, serverId) as? Step.Stop)?.let { return AppResult.Failure(it.error) }

        // 3. Pausas (solo mientras la API tiene el viaje en curso)
        trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (!trip.closedOnServer) {
            (pauses.send(trip, serverId) as? Step.Stop)?.let { return AppResult.Failure(it.error) }
        }

        // 4. Recargas
        val fuel = store.current().fuel.filter {
            it.tripLocalId == localId && it.state != FuelSyncState.SYNCED && !SyncRejection.isPermanent(it.syncErrorCode)
        }
        for (entity in fuel) {
            (fuelLoads.sync(entity, serverId) as? Step.Stop)?.let { return AppResult.Failure(it.error) }
        }

        // 5. Cierre (solo cuando el usuario ya guardó "Registrar Viaje")
        trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (!trip.running && !trip.closedOnServer && !trip.isBlocked()) {
            (trips.finish(trip, serverId) as? Step.Stop)?.let { return AppResult.Failure(it.error) }
        }

        // 6. Foto del odómetro
        trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (trip.closedOnServer && trip.odometerPhotoPath != null && !trip.odometerPhotoSent) {
            (trips.uploadOdometerPhoto(trip, serverId) as? Step.Stop)?.let { return AppResult.Failure(it.error) }
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
    }

    // ---------------- Utilidades ----------------

    private suspend fun currentTrip(localId: String): TripEntity? =
        store.current().trips.firstOrNull { it.localId == localId }
}
