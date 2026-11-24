package org.vicpoo.gasolineproject.domain.profile.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.profile.ProfileRepository

class RefreshProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}
