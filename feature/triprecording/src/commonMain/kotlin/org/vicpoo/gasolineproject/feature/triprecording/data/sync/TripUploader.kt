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
                Step.Rejected
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
