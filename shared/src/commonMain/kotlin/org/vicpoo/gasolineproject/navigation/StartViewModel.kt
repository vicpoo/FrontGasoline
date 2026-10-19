package org.vicpoo.gasolineproject.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.AuthStatus
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.GetAuthStatusUseCase

/** Decide la primera pantalla según la sesión guardada (funciona sin red). */
class StartViewModel(
    getAuthStatus: GetAuthStatusUseCase,
    sessionReader: SessionReader,
) : ViewModel() {

    val startRoute: StateFlow<AppRoute?> = flow {
        emit(
            when (getAuthStatus()) {
                AuthStatus.LoggedOut -> AppRoute.Welcome
                AuthStatus.NeedsLicense -> AppRoute.UploadLicense
                AuthStatus.LoggedIn -> AppRoute.Home
            },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** false cuando no hay sesión (cerró sesión o el token venció -> 401). */
    val isLoggedIn: StateFlow<Boolean?> = sessionReader.session
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

val navigationModule = module {
    viewModelOf(::StartViewModel)
    viewModelOf(::MainViewModel)
}
