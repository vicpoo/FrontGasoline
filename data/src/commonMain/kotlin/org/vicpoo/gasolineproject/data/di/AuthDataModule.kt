package org.vicpoo.gasolineproject.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.auth.remote.AuthApi
import org.vicpoo.gasolineproject.data.auth.remote.KtorAuthApi
import org.vicpoo.gasolineproject.data.auth.repository.AuthRepositoryImpl
import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository
import org.vicpoo.gasolineproject.domain.session.LogoutListener

val authDataModule = module {
    singleOf(::KtorAuthApi) { bind<AuthApi>() }
    // Explícito: recibe TODOS los LogoutListener de las features (getAll), eso no se resuelve por tipo
    single<AuthRepository> {
        AuthRepositoryImpl(
            api = get(),
            sessionReader = get(),
            sessionWriter = get(),
            tokenManager = get(),
            sessionApi = get(),
            logoutListeners = getAll<LogoutListener>(),
        )
    }
}
