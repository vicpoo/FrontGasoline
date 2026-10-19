package org.vicpoo.gasolineproject.feature.auth.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.feature.auth.data.remote.AuthApi
import org.vicpoo.gasolineproject.feature.auth.data.remote.KtorAuthApi
import org.vicpoo.gasolineproject.feature.auth.data.repository.AuthRepositoryImpl
import org.vicpoo.gasolineproject.feature.auth.domain.repository.AuthRepository
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.GetAuthStatusUseCase
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.LoginUseCase
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.LogoutUseCase
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.RefreshPrivilegesUseCase
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.UploadLicenseUseCase
import org.vicpoo.gasolineproject.feature.auth.domain.validation.LoginValidator
import org.vicpoo.gasolineproject.feature.auth.presentation.license.LicenseViewModel
import org.vicpoo.gasolineproject.feature.auth.presentation.login.LoginViewModel

val authModule = module {
    // Data
    single<AuthApi> { KtorAuthApi(get()) }
    single<AuthRepository> {
        AuthRepositoryImpl(
            api = get(),
            sessionReader = get(),
            sessionWriter = get(),
            logoutListeners = getAll<LogoutListener>(),
        )
    }

    // Domain
    factoryOf(::LoginValidator)
    factoryOf(::LoginUseCase)
    factoryOf(::UploadLicenseUseCase)
    factoryOf(::RefreshPrivilegesUseCase)
    factoryOf(::LogoutUseCase)
    factoryOf(::GetAuthStatusUseCase)

    // Presentation
    viewModelOf(::LoginViewModel)
    viewModelOf(::LicenseViewModel)
}
