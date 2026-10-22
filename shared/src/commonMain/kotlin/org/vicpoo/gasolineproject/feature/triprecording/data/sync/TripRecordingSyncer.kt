package org.vicpoo.gasolineproject.feature.triprecording.data.sync

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.httpCode
import org.vicpoo.gasolineproject.core.common.serverMessage
import org.vicpoo.gasolineproject.core.di.AppScope
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
            when (val created = createOnServer(trip)) {
                is Step.Stop -> return AppResult.Failure(created.error)
                is Step.Rejected -> return AppResult.Success(Unit) // el error quedó guardado en el viaje
                is Step.Done -> trip = currentTrip(localId) ?: return AppResult.Success(Unit)
            }
        }
        val serverId = trip.serverId ?: return AppResult.Success(Unit)

        // 2. Puntos GPS
        (sendPoints(trip, serverId) as? Step.Stop)?.let { return AppResult.Failure(it.error) }

        // 3. Pausas (solo mientras la API tiene el viaje en curso)
        trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (!trip.closedOnServer) {
            (sendPauses(trip, serverId) as? Step.Stop)?.let { return AppResult.Failure(it.error) }
        }

        // 4. Recargas
        val fuel = store.current().fuel.filter {
            it.tripLocalId == localId && it.state != FuelSyncState.SYNCED && !SyncRejection.isPermanent(it.syncErrorCode)
        }
        for (entity in fuel) {
            (syncFuel(entity, serverId) as? Step.Stop)?.let { return AppResult.Failure(it.error) }
        }

        // 5. Cierre (solo cuando el usuario ya guardó "Registrar Viaje")
        trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (!trip.running && !trip.closedOnServer && !trip.isBlocked()) {
            (finishOnServer(trip, serverId) as? Step.Stop)?.let { return AppResult.Failure(it.error) }
        }

        // 6. Foto del odómetro
        trip = currentTrip(localId) ?: return AppResult.Success(Unit)
        if (trip.closedOnServer && trip.odometerPhotoPath != null && !trip.odometerPhotoSent) {
            (uploadOdometerPhoto(trip, serverId) as? Step.Stop)?.let { return AppResult.Failure(it.error) }
        }

        // 7. Limpieza
        cleanupIfDone(localId)
        return AppResult.Success(Unit)
    }

    // ---------------- 1. Viaje ----------------

    private suspend fun createOnServer(trip: TripEntity): Step {
        // Mientras el usuario no guarde "Registrar Viaje" se crea en curso (así ya suben los puntos)
        val completeOffline = !trip.running && trip.finalMileage != null && trip.endedAt != null
        val result = if (!completeOffline) {
            safeApiCall {
                api.startTrip(
                    IniciarViajeRequestDto(
                        empresaId = trip.companyId,
                        carroId = trip.vehicle.vehicleId,
                        solicitudReservaId = trip.vehicle.reservationId,
                        empleadoOperadorId = trip.employeeId,
                        fechaInicio = trip.startedAt,
                        ubicacionInicioLat = trip.startLat?.round7(),
                        ubicacionInicioLng = trip.startLng?.round7(),
                        kilometrajeInicial = trip.initialMileage.round2(),
                        centroCostoId = trip.costCenter?.id,
                    ),
                )
            }
        } else {
            // Todo el viaje se hizo sin red: se manda completo de una vez
            safeApiCall {
                api.syncFinishedTrip(
                    SincronizarViajeRequestDto(
                        empresaId = trip.companyId,
                        carroId = trip.vehicle.vehicleId,
                        solicitudReservaId = trip.vehicle.reservationId,
                        empleadoOperadorId = trip.employeeId,
                        fechaInicio = trip.startedAt,
                        ubicacionInicioLat = trip.startLat?.round7(),
                        ubicacionInicioLng = trip.startLng?.round7(),
                        kilometrajeInicial = trip.initialMileage.round2(),
                        estado = "finalizado",
                        fechaFin = trip.endedAt,
                        ubicacionFinLat = trip.endLat?.round7(),
                        ubicacionFinLng = trip.endLng?.round7(),
                        kilometrajeFinal = trip.finalMileage(),
                        centroCostoId = trip.costCenter?.id,
                        pausas = trip.pauses.map { it.toSyncDto() }.ifEmpty { null },
                    ),
                )
            }
        }
        return when (result) {
            is AppResult.Success -> {
                store.updateTrip(trip.localId) { entity ->
                    entity.copy(
                        serverId = result.data.viajeId,
                        closedOnServer = completeOffline,
                        // Con /sincronizar las pausas ya viajaron junto con el viaje
                        pauses = if (completeOffline) entity.pauses.map { it.copy(startSent = true, endSent = true) } else entity.pauses,
                        syncErrorMessage = null,
                        syncErrorCode = null,
                    )
                }
                Step.Done
            }
            is AppResult.Failure -> handleTripFailure(trip.localId, result.error)
        }
    }

    // ---------------- 2. Puntos ----------------

    private suspend fun sendPoints(trip: TripEntity, serverId: String): Step {
        var sent = trip.pointsSent
        while (true) {
            val batch = store.readPoints(trip.localId, from = sent, limit = POINTS_BATCH)
            if (batch.isEmpty()) return Step.Done
            when (val result = safeApiCall { api.sendRoutePoints(serverId, batch.map { it.toRequest() }) }) {
                is AppResult.Success -> {
                    sent += batch.size
                    val total = sent
                    store.updateTrip(trip.localId) { it.copy(pointsSent = total) }
                }
                is AppResult.Failure -> {
                    if (result.error.stopsSync()) return Step.Stop(result.error)
                    // Error temporal (5xx): se intenta en la próxima vuelta
                    if (!SyncRejection.isPermanent(result.error.httpCode())) return Step.Rejected
                    // Rechazo definitivo (400): reenviarlo daría lo mismo. Se salta el lote y se cuenta
                    sent += batch.size
                    val total = sent
                    store.updateTrip(trip.localId) { it.copy(pointsSent = total, pointsDiscarded = it.pointsDiscarded + batch.size) }
                }
            }
        }
    }

    // ---------------- 3. Pausas ----------------

    private suspend fun sendPauses(trip: TripEntity, serverId: String): Step {
        for (pause in trip.pauses) {
            if (!pause.startSent) {
                val request = PausarViajeRequestDto(
                    fecha = pause.startedAt,
                    motivo = pause.reason,
                    ubicacionLat = pause.lat?.round7(),
                    ubicacionLng = pause.lng?.round7(),
                )
                when (val result = safeApiCall { api.pauseTrip(serverId, request) }) {
                    is AppResult.Success -> markPause(trip.localId, pause.localId) { it.copy(startSent = true) }
                    is AppResult.Failure -> {
                        if (result.error.stopsSync()) return Step.Stop(result.error)
                        // 400 "ya está en pausa": la llamada anterior sí llegó aunque se perdió la respuesta
                        markPause(trip.localId, pause.localId) { it.copy(startSent = true) }
                    }
                }
            }
            if (pause.endedAt != null && !pause.endSent) {
                when (val result = safeApiCall { api.resumeTrip(serverId, ReanudarViajeRequestDto(fecha = pause.endedAt)) }) {
                    is AppResult.Success -> markPause(trip.localId, pause.localId) { it.copy(endSent = true) }
                    is AppResult.Failure -> {
                        if (result.error.stopsSync()) return Step.Stop(result.error)
                        // 400 "no está en pausa": ya se había reanudado
                        markPause(trip.localId, pause.localId) { it.copy(endSent = true) }
                    }
                }
            }
        }
        return Step.Done
    }

    private suspend fun markPause(tripLocalId: String, pauseLocalId: String, transform: (PauseEntity) -> PauseEntity) {
        store.updateTrip(tripLocalId) { entity ->
            entity.copy(pauses = entity.pauses.map { if (it.localId == pauseLocalId) transform(it) else it })
        }
    }

    // ---------------- 4. Recargas ----------------

    private suspend fun syncFuel(entity: FuelEntity, tripServerId: String): Step = when {
        entity.state == FuelSyncState.PENDING_DELETE -> deleteFuel(entity)
        entity.serverId == null -> createFuel(entity, tripServerId)
        else -> updateFuel(entity, entity.serverId)
    }

    private suspend fun createFuel(entity: FuelEntity, tripServerId: String): Step {
        val receipt = receiptOf(entity)
            ?: return markFuelError(entity.localId, MISSING_RECEIPT, SyncRejection.LOCAL_INVALID)
        val result = safeApiCall { api.createFuelLoad(entity.toForm(viajeId = tripServerId, motivo = null, receipt = receipt)) }
        return when (result) {
            is AppResult.Success -> {
                store.updateFuel(entity.localId) {
                    it.copy(
                        serverId = result.data.cargaCombustibleId,
                        receiptUrl = result.data.fotoComprobanteUrl,
                        receiptChanged = false,
                        state = FuelSyncState.SYNCED,
                        syncErrorMessage = null,
                        syncErrorCode = null,
                    )
                }
                Step.Done
            }
            is AppResult.Failure -> handleFuelFailure(entity.localId, result.error)
        }
    }

    private suspend fun updateFuel(entity: FuelEntity, serverId: String): Step {
        val receipt = if (entity.receiptChanged) receiptOf(entity) else null
        val form = entity.toForm(viajeId = null, motivo = CORRECTION_REASON, receipt = receipt)
        return when (val result = safeApiCall { api.updateFuelLoad(serverId, form) }) {
            is AppResult.Success -> {
                store.updateFuel(entity.localId) {
                    it.copy(
                        receiptUrl = result.data.fotoComprobanteUrl ?: it.receiptUrl,
                        receiptChanged = false,
                        state = FuelSyncState.SYNCED,
                        syncErrorMessage = null,
                        syncErrorCode = null,
                    )
                }
                Step.Done
            }
            is AppResult.Failure -> handleFuelFailure(entity.localId, result.error)
        }
    }

    private suspend fun deleteFuel(entity: FuelEntity): Step {
        val serverId = entity.serverId
        if (serverId != null) {
            val result = safeApiCall { api.deleteFuelLoad(serverId, DELETE_REASON) }
            // 404 = ya no existe en la API: igual se borra en el teléfono
            if (result is AppResult.Failure && result.error != AppError.NotFound) {
                if (result.error.stopsSync()) return Step.Stop(result.error)
                // La API no lo dejó borrar: vuelve a aparecer en la lista con el mensaje
                store.updateFuel(entity.localId) {
                    it.copy(
                        state = FuelSyncState.SYNCED,
                        syncErrorMessage = result.error.messageOrDefault(),
                        syncErrorCode = result.error.httpCode(),
                    )
                }
                return Step.Rejected
            }
        }
        store.update { snapshot -> snapshot.copy(fuel = snapshot.fuel.filterNot { it.localId == entity.localId }) }
        store.deleteFile(entity.receiptPath)
        return Step.Done
    }

    // ---------------- 5. Cierre ----------------

    private suspend fun finishOnServer(trip: TripEntity, serverId: String): Step {
        val request = FinalizarViajeRequestDto(
            fechaFin = trip.endedAt ?: trip.startedAt,
            ubicacionFinLat = trip.endLat?.round7(),
            ubicacionFinLng = trip.endLng?.round7(),
            kilometrajeFinal = trip.finalMileage(),
        )
        return when (val result = safeApiCall { api.finishTrip(serverId, request) }) {
            is AppResult.Success -> {
                store.updateTrip(trip.localId) { it.copy(closedOnServer = true, syncErrorMessage = null, syncErrorCode = null) }
                Step.Done
            }
            is AppResult.Failure -> {
                // 409 "solo se puede finalizar un viaje en curso": ya estaba cerrado en la API
                if (result.error is AppError.Server && result.error.code == 409) {
                    store.updateTrip(trip.localId) { it.copy(closedOnServer = true, syncErrorMessage = null, syncErrorCode = null) }
                    Step.Done
                } else {
                    handleTripFailure(trip.localId, result.error)
                }
            }
        }
    }

    // ---------------- 6. Foto del odómetro ----------------

    private suspend fun uploadOdometerPhoto(trip: TripEntity, serverId: String): Step {
        val path = trip.odometerPhotoPath ?: return Step.Done
        val bytes = store.readReceipt(path)
        if (bytes == null) {
            // El archivo ya no existe en el teléfono: no hay nada que subir
            store.updateTrip(trip.localId) { it.copy(odometerPhotoSent = true, odometerPhotoPath = null) }
            return Step.Done
        }
        val extension = path.substringAfterLast('.', "jpg")
        val upload = ReceiptUpload(bytes = bytes, fileName = "odometro-final.$extension", mimeType = trip.odometerPhotoMimeType ?: "image/jpeg")
        return when (val result = safeApiCall { api.uploadDashboardPhoto(serverId, MOMENTO_FINAL, upload) }) {
            is AppResult.Success -> {
                store.updateTrip(trip.localId) { it.copy(odometerPhotoSent = true) }
                Step.Done
            }
            is AppResult.Failure -> {
                if (result.error.stopsSync()) return Step.Stop(result.error)
                if (SyncRejection.isPermanent(result.error.httpCode())) {
                    // La API no acepta esta foto (ej. formato o tamaño): no se reintenta siempre.
                    // El viaje ya está cerrado en la API; la foto se puede corregir desde la web.
                    store.updateTrip(trip.localId) { it.copy(odometerPhotoSent = true) }
                    return Step.Done
                }
                // Error temporal: se vuelve a intentar en la próxima sincronización
                Step.Rejected
            }
        }
    }

    // ---------------- 7. Limpieza ----------------

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

    /** El viaje tiene un rechazo definitivo pendiente de que el usuario lo corrija. */
    private fun TripEntity.isBlocked(): Boolean =
        syncErrorMessage != null && SyncRejection.isPermanent(syncErrorCode)

    private suspend fun receiptOf(entity: FuelEntity): ReceiptUpload? {
        val path = entity.receiptPath ?: return null
        val bytes = store.readReceipt(path) ?: return null
        val mime = entity.receiptMimeType ?: "image/jpeg"
        val extension = path.substringAfterLast('.', "jpg")
        return ReceiptUpload(bytes = bytes, fileName = "recibo-${entity.number}.$extension", mimeType = mime)
    }

    private suspend fun handleTripFailure(localId: String, error: AppError): Step {
        if (error.stopsSync()) return Step.Stop(error)
        store.updateTrip(localId) { it.copy(syncErrorMessage = error.messageOrDefault(), syncErrorCode = error.httpCode()) }
        return Step.Rejected
    }

    private suspend fun handleFuelFailure(localId: String, error: AppError): Step {
        if (error.stopsSync()) return Step.Stop(error)
        return markFuelError(localId, error.messageOrDefault(), error.httpCode())
    }

    private suspend fun markFuelError(localId: String, message: String, code: Int?): Step {
        store.updateFuel(localId) { it.copy(syncErrorMessage = message, syncErrorCode = code) }
        return Step.Rejected
    }

    private sealed interface Step {
        data object Done : Step
        data object Rejected : Step
        data class Stop(val error: AppError) : Step
    }

    private companion object {
        const val POINTS_BATCH = 500
        const val MOMENTO_FINAL = "final"
        const val CORRECTION_REASON = "Corrección desde la app móvil por el operador del viaje."
        const val DELETE_REASON = "Eliminada desde la app móvil por el operador del viaje."
        const val MISSING_RECEIPT = "Falta la foto del recibo. Edita la recarga y vuelve a subirla."
        const val DEFAULT_REJECTED = "El servidor rechazó el registro."
    }

    private fun AppError.messageOrDefault(): String = serverMessage() ?: DEFAULT_REJECTED
}

// ---------------- Mapeos a los contratos de la API ----------------

private fun TrackPointEntity.toRequest() = PuntoRutaRequestDto(
    latitud = lat.round7(),
    longitud = lng.round7(),
    timestampCaptura = t,
    precisionMetros = acc?.coerceIn(0.0, 9999.99)?.round2(),
    altitud = alt?.coerceIn(-500.0, 9000.0)?.round2(),
    velocidad = spd?.coerceIn(0.0, 9999.99)?.round2(),
)

private fun FuelEntity.toForm(viajeId: String?, motivo: String?, receipt: ReceiptUpload?) = CargaFormData(
    viajeId = viajeId,
    litros = liters,
    monto = amount,
    fechaCarga = loadedAt,
    ubicacionLat = lat,
    ubicacionLng = lng,
    motivo = motivo,
    centroCostoId = costCenter?.id,
    receipt = receipt,
)

/**
 * Kilometraje final: el que capturó el usuario en "Registrar Viaje" (odómetro).
 * Si por algún motivo no existe, se estima con el inicial + la distancia del GPS.
 */
private fun TripEntity.finalMileage(): Double =
    (finalMileage ?: (initialMileage + distanceMeters / 1000.0)).round2()

private fun PauseEntity.toSyncDto() = PausaSincronizadaDto(
    fechaInicio = startedAt,
    fechaFin = endedAt,
    motivo = reason,
    ubicacionLat = lat?.round7(),
    ubicacionLng = lng?.round7(),
)

private fun Double.round2() = round(this * 100) / 100
private fun Double.round7() = round(this * 10_000_000) / 10_000_000
