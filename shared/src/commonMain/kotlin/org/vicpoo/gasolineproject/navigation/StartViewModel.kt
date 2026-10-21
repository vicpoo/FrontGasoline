package org.vicpoo.gasolineproject.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.AuthStatus
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.GetAuthStatusUseCase
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.ObserveIsLoggedInUseCase

/** Decide por dónde entra la app según la sesión guardada (funciona sin red). */
class StartViewModel(
    getAuthStatus: GetAuthStatusUseCase,
    observeIsLoggedIn: ObserveIsLoggedInUseCase,
) : ViewModel() {

    /** null mientras se lee la sesión guardada. */
    val startStatus: StateFlow<AuthStatus?> = flow { emit(getAuthStatus()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** false cuando no hay sesión (cerró sesión o el token venció -> 401). */
    val isLoggedIn: StateFlow<Boolean?> = observeIsLoggedIn()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
