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
 * Paso 2: sube los puntos GPS en lotes. La API ignora duplicados, así que reenviar un lote es seguro.
 * Un lote rechazado de forma definitiva (400) se descarta y se cuenta, para no bloquear el viaje.
 */
internal class RoutePointsUploader(
    private val store: TripRecordingLocalStore,
    private val api: TripRecordingApi,
) {

    suspend fun send(trip: TripEntity, serverId: String): Step {
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

    private companion object {
        const val POINTS_BATCH = 500
    }
}
