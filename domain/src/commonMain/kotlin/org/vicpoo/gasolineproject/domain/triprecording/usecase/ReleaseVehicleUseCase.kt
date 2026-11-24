package org.vicpoo.gasolineproject.domain.triprecording.usecase

import kotlinx.coroutines.flow.first
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicleRepository
import org.vicpoo.gasolineproject.domain.triprecording.ReleaseBlock

/**
 * "Dejar carro": el empleado ya no lo necesita y queda disponible para otros.
 * Se revisa primero en el teléfono (viaje en curso o sin enviar); la API lo vuelve a validar.
 * Las observaciones son opcionales (máx. 500).
 */
class ReleaseVehicleUseCase(private val repository: AssignedVehicleRepository) {
    suspend operator fun invoke(reservationId: String, notes: String): AppResult<Unit> {
        val clean = notes.trim().takeIf { it.isNotEmpty() }
        if (clean != null && clean.length > MAX_NOTES) return AppResult.Failure(AppError.Validation(ValidationReason.OUT_OF_RANGE))
        // Mismo código que la API (409); la pantalla muestra el motivo con ReleaseBlock
        if (repository.observeReleaseBlocks().first().containsKey(reservationId)) {
            return AppResult.Failure(AppError.Server(409, null))
        }
        return repository.release(reservationId, clean)
    }

    companion object {
        const val MAX_NOTES = 500
    }
}
