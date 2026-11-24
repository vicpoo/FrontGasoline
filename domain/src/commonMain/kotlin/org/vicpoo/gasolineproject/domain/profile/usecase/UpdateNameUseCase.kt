package org.vicpoo.gasolineproject.domain.profile.usecase

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.profile.ProfileRepository

/** Nombre completo: obligatorio, máx. 150 caracteres (igual que la API). */
class UpdateNameUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(name: String): AppResult<Unit> {
        val clean = name.trim()
        if (clean.isEmpty()) return AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED))
        if (clean.length > MAX_NAME) return AppResult.Failure(AppError.Validation(ValidationReason.OUT_OF_RANGE))
        return repository.updateName(clean)
    }

    companion object {
        const val MAX_NAME = 150
    }
}
