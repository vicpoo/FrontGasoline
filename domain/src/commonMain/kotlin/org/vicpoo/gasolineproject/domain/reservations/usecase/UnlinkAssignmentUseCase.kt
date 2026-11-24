package org.vicpoo.gasolineproject.domain.reservations.usecase

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.reservations.AssignmentsRepository

/** Desvincular: el motivo es obligatorio (le llega al empleado en la notificación). */
class UnlinkAssignmentUseCase(private val repository: AssignmentsRepository) {
    suspend operator fun invoke(reservationId: String, reason: String): AppResult<Unit> {
        val clean = reason.trim()
        if (clean.isEmpty()) return AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED))
        if (clean.length > MAX_REASON) return AppResult.Failure(AppError.Validation(ValidationReason.OUT_OF_RANGE))
        return repository.unlink(reservationId, clean)
    }

    companion object {
        const val MAX_REASON = 255
    }
}
