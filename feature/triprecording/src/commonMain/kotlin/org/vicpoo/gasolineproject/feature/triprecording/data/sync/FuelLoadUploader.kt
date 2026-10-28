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

/** Paso 4: crea, corrige o borra en la API las recargas de combustible del viaje (con la foto del recibo). */
internal class FuelLoadUploader(
    private val store: TripRecordingLocalStore,
    private val api: TripRecordingApi,
) {

    suspend fun sync(entity: FuelEntity, tripServerId: String): Step = when {
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

    private suspend fun receiptOf(entity: FuelEntity): ReceiptUpload? {
        val path = entity.receiptPath ?: return null
        val bytes = store.readReceipt(path) ?: return null
        val mime = entity.receiptMimeType ?: "image/jpeg"
        val extension = path.substringAfterLast('.', "jpg")
        return ReceiptUpload(bytes = bytes, fileName = "recibo-${entity.number}.$extension", mimeType = mime)
    }

    private suspend fun handleFuelFailure(localId: String, error: AppError): Step {
        if (error.stopsSync()) return Step.Stop(error)
        return markFuelError(localId, error.messageOrDefault(), error.httpCode())
    }

    private suspend fun markFuelError(localId: String, message: String, code: Int?): Step {
        store.updateFuel(localId) { it.copy(syncErrorMessage = message, syncErrorCode = code) }
        return Step.Rejected
    }

    private companion object {
        const val CORRECTION_REASON = "Corrección desde la app móvil por el operador del viaje."
        const val DELETE_REASON = "Eliminada desde la app móvil por el operador del viaje."
        const val MISSING_RECEIPT = "Falta la foto del recibo. Edita la recarga y vuelve a subirla."
    }
}
