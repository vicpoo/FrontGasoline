package org.vicpoo.gasolineproject.domain.triprecording.usecase

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.triprecording.CostCenterRepository
import org.vicpoo.gasolineproject.domain.triprecording.FuelField
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordDraft
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordRepository
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordValidator
import org.vicpoo.gasolineproject.domain.triprecording.FuelValidation
import org.vicpoo.gasolineproject.domain.triprecording.SEND_WAIT_MS
import org.vicpoo.gasolineproject.domain.triprecording.SaveFuelResult
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker
import org.vicpoo.gasolineproject.domain.triprecording.outcomeFor

/**
 * Guarda la recarga en el teléfono (offline-first) y luego intenta enviarla.
 * - Guardar es NonCancellable: aunque el usuario salga de la pantalla, la recarga queda guardada.
 * - El envío corre en segundo plano (scope de la app) y aquí solo se espera unos segundos
 *   ([SEND_WAIT_MS]); con señal débil sigue enviándose solo y la pantalla dice "guardada".
 */
class SaveFuelRecordUseCase(
    private val repository: FuelRecordRepository,
    private val sync: TripRecordingSync,
    private val tracker: TripTracker,
    private val costCenters: CostCenterRepository,
    private val validator: FuelRecordValidator,
    private val imageCompressor: ImageCompressor,
    private val captureLocation: CaptureFuelLocationUseCase,
) {
    suspend operator fun invoke(draft: FuelRecordDraft): SaveFuelResult =
        withContext(NonCancellable) { save(draft) }

    private suspend fun save(draft: FuelRecordDraft): SaveFuelResult {
        val trip = tracker.session.value.trip
            ?: return SaveFuelResult.Failed(AppError.NotFound)

        val centers = costCenters.observe().first()
        val receipt = draft.newReceipt?.let { imageCompressor.compress(it) }
        if (draft.newReceipt != null && receipt == null) {
            // El recibo no se pudo recodificar: se rechaza (no se sube la original con su EXIF)
            val otherErrors = (validator.validate(draft, centers, trip.vehicle.tankCapacityLiters) as? FuelValidation.Invalid)
                ?.errors.orEmpty()
            return SaveFuelResult.Invalid(otherErrors + (FuelField.RECEIPT to ValidationReason.INVALID_FILE_TYPE))
        }
        val prepared = draft.copy(newReceipt = receipt)
        val values = when (
            val validation = validator.validate(prepared, centers, trip.vehicle.tankCapacityLiters)
        ) {
            is FuelValidation.Invalid -> return SaveFuelResult.Invalid(validation.errors)
            is FuelValidation.Valid -> validation.values
        }

        // Solo al crear: la ubicación de una carga ya registrada no se mueve al editarla
        val location = if (draft.localId == null) draft.location ?: captureLocation() else null

        val saved = when (val result = repository.save(trip.localId, draft.localId, values, receipt, location)) {
            is AppResult.Failure -> return SaveFuelResult.Failed(result.error)
            is AppResult.Success -> result.data
        }
        return SaveFuelResult.Saved(recordId = saved.localId, outcome = sendAndReport(saved.localId))
    }

    private suspend fun sendAndReport(localId: String): SendOutcome =
        sync.outcomeFor(localId) { repository.get(it) }
}
