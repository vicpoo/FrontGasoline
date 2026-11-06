package org.vicpoo.gasolineproject.data.triprecording.sync

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.httpCode
import org.vicpoo.gasolineproject.domain.common.serverMessage
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.data.network.safeApiCall
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncScheduler
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
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import kotlin.math.round

/**
 * Pasos 1, 5 y 6 de la sincronización de un viaje: crearlo en la API (en curso o completo si se
 * hizo sin red), cerrarlo con el kilometraje final y subir la foto del odómetro.
 */
internal class TripUploader(
    private val store: TripRecordingLocalStore,
    private val api: TripRecordingApi,
) {

    suspend fun create(trip: TripEntity): Step {
        // Mientras el usuario no guarde "Registrar Viaje" se crea en curso (así ya suben los puntos)
        val completeOffline = !trip.running && trip.finalMileage != null && trip.endedAt != null
        // Offline-first: el viaje viaja con el UUID que generó el teléfono. Si la respuesta se pierde y se
        // reenvía, la API regresa el mismo viaje en lugar de "el carro ya tiene un viaje en curso".
        val clientId = trip.localId.asUuidOrNull()
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
                        viajeId = clientId,
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
                        viajeId = clientId,
                    ),
                )
            }
        }
        return when (result) {
            is AppResult.Success -> {
                // Manda lo que diga la API: en un reintento /sincronizar puede regresar el viaje que ya se
                // había creado EN CURSO (se perdió la respuesta de POST /viajes). Entonces todavía faltan
                // sus pausas, recargas y el finalizar, como en un viaje en línea.
                val closed = result.data.estado?.let { it != ESTADO_EN_CURSO } ?: completeOffline
                store.updateTrip(trip.localId) { entity ->
                    entity.copy(
                        serverId = result.data.viajeId,
                        closedOnServer = closed,
                        // Con /sincronizar las pausas ya viajaron junto con el viaje
                        pauses = if (completeOffline && closed) entity.pauses.map { it.copy(startSent = true, endSent = true) } else entity.pauses,
                        syncErrorMessage = null,
                        syncErrorCode = null,
                    )
                }
                Step.Done
            }
            is AppResult.Failure -> adoptServerTrip(trip, result.error) ?: handleTripFailure(trip.localId, result.error)
        }
    }

    /**
     * 409 VIAJE_EN_CURSO: el carro ya tiene un viaje abierto en la API. Si es ESTE mismo viaje (una versión
     * anterior de la app lo creó sin mandar su UUID y se perdió la respuesta), se liga a ese id y se sigue
     * con los puntos, pausas y recargas. Si es otro viaje regresa null y queda el error para el usuario.
     */
    private suspend fun adoptServerTrip(trip: TripEntity, error: AppError): Step? {
        val server = error as? AppError.Server ?: return null
        if (server.problemCode != CODIGO_VIAJE_EN_CURSO) return null
        val serverTripId = server.problemData["viajeId"] ?: return null
        val sameTrip = ServerTripMatch.isSameTrip(
            employeeId = trip.employeeId,
            startedAt = trip.startedAt,
            serverEmployeeId = server.problemData["empleadoOperadorId"],
            serverStartedAt = server.problemData["fechaInicio"],
        )
        if (!sameTrip) return null

        store.updateTrip(trip.localId) {
            it.copy(serverId = serverTripId, closedOnServer = false, syncErrorMessage = null, syncErrorCode = null)
        }
        return Step.Done
    }


    suspend fun finish(trip: TripEntity, serverId: String): Step {
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
                // AppError vive en :core:common: no se puede hacer smart cast de result.error desde otro módulo
                if ((result.error as? AppError.Server)?.code == 409) {
                    store.updateTrip(trip.localId) { it.copy(closedOnServer = true, syncErrorMessage = null, syncErrorCode = null) }
                    Step.Done
                } else {
                    handleTripFailure(trip.localId, result.error)
                }
            }
        }
    }


    suspend fun uploadOdometerPhoto(trip: TripEntity, serverId: String): Step {
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
                Step.Deferred(result.error)
            }
        }
    }

    private suspend fun handleTripFailure(localId: String, error: AppError): Step {
        if (error.stopsSync()) return Step.Stop(error)
        store.updateTrip(localId) { it.copy(syncErrorMessage = error.messageOrDefault(), syncErrorCode = error.httpCode()) }
        return Step.Rejected
    }

    private companion object {
        const val MOMENTO_FINAL = "final"
    }
}
