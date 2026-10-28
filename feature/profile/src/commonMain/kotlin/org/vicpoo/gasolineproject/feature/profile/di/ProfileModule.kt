package org.vicpoo.gasolineproject.feature.profile.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.feature.profile.data.ProfileRepositoryImpl
import org.vicpoo.gasolineproject.feature.profile.data.remote.KtorProfileApi
import org.vicpoo.gasolineproject.feature.profile.data.remote.ProfileApi
import org.vicpoo.gasolineproject.feature.profile.domain.ObserveProfileUseCase
import org.vicpoo.gasolineproject.feature.profile.domain.ProfileRepository
import org.vicpoo.gasolineproject.feature.profile.domain.RefreshProfileUseCase
import org.vicpoo.gasolineproject.feature.profile.domain.UpdateNameUseCase
import org.vicpoo.gasolineproject.feature.profile.domain.UpdateProfilePhotoUseCase
import org.vicpoo.gasolineproject.feature.profile.presentation.ProfileViewModel
import org.vicpoo.gasolineproject.feature.profile.presentation.editname.EditNameViewModel

val profileModule = module {
    // Data
    singleOf(::KtorProfileApi) { bind<ProfileApi>() }
    singleOf(::ProfileRepositoryImpl) { bind<ProfileRepository>() }

    // Domain
    factoryOf(::ObserveProfileUseCase)
    factoryOf(::RefreshProfileUseCase)
    factoryOf(::UpdateNameUseCase)
    factoryOf(::UpdateProfilePhotoUseCase)

    // Presentation
    viewModelOf(::ProfileViewModel)
    viewModelOf(::EditNameViewModel)
}
