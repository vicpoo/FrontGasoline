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

/** Paso 3: manda las pausas y reanudaciones en orden, con la hora del teléfono. */
internal class PauseUploader(
    private val store: TripRecordingLocalStore,
    private val api: TripRecordingApi,
) {

    suspend fun send(trip: TripEntity, serverId: String): Step {
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
}
