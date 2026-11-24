package org.vicpoo.gasolineproject.domain.profile.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.profile.Profile
import org.vicpoo.gasolineproject.domain.profile.ProfileRepository

class ObserveProfileUseCase(private val repository: ProfileRepository) {
    operator fun invoke(): Flow<Profile?> = repository.observeProfile()
}
