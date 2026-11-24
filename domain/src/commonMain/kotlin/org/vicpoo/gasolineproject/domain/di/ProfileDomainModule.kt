package org.vicpoo.gasolineproject.domain.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.profile.usecase.ObserveProfileUseCase
import org.vicpoo.gasolineproject.domain.profile.usecase.RefreshProfileUseCase
import org.vicpoo.gasolineproject.domain.profile.usecase.UpdateNameUseCase
import org.vicpoo.gasolineproject.domain.profile.usecase.UpdateProfilePhotoUseCase

val profileDomainModule = module {
    factoryOf(::ObserveProfileUseCase)
    factoryOf(::RefreshProfileUseCase)
    factoryOf(::UpdateNameUseCase)
    factoryOf(::UpdateProfilePhotoUseCase)
}
