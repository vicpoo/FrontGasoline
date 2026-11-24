package org.vicpoo.gasolineproject.domain.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.auth.usecase.GetAuthStatusUseCase
import org.vicpoo.gasolineproject.domain.auth.usecase.LoginUseCase
import org.vicpoo.gasolineproject.domain.auth.usecase.LogoutUseCase
import org.vicpoo.gasolineproject.domain.auth.usecase.ObserveIsLoggedInUseCase
import org.vicpoo.gasolineproject.domain.auth.usecase.RefreshPrivilegesUseCase
import org.vicpoo.gasolineproject.domain.auth.validation.LoginValidator
import org.vicpoo.gasolineproject.domain.session.LogoutAction

val authDomainModule = module {
    factoryOf(::LoginValidator)
    factoryOf(::LoginUseCase)
    factoryOf(::RefreshPrivilegesUseCase)
    // Otras features (Perfil) cierran sesión por el contrato LogoutAction
    factoryOf(::LogoutUseCase) bind LogoutAction::class
    factoryOf(::GetAuthStatusUseCase)
    factoryOf(::ObserveIsLoggedInUseCase)
}
