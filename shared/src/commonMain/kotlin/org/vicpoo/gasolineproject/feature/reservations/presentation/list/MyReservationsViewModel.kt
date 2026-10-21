package org.vicpoo.gasolineproject.feature.reservations.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.feature.reservations.domain.MarkReservationUpdatesSeenUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.ObserveMyReservationsUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.RefreshMyReservationsUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRequest

data class MyReservationsUiState(
    val requests: List<ReservationRequest> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Sin conexión: se muestra la última copia guardada. */
    val isOffline: Boolean = false,
    val error: AppError? = null,
)

sealed interface MyReservationsEvent {
    data object Refresh : MyReservationsEvent
}

/** Campana de Inicio: las solicitudes que envió el usuario y su estado (solo para ver cómo van). */
class MyReservationsViewModel(
    observeRequests: ObserveMyReservationsUseCase,
    private val refreshRequests: RefreshMyReservationsUseCase,
    private val markUpdatesSeen: MarkReservationUpdatesSeenUseCase,
) : ViewModel() {

    private data class LoadState(
        val firstLoadDone: Boolean = false,
        val refreshing: Boolean = false,
        val offline: Boolean = false,
        val error: AppError? = null,
    )

    private val loading = MutableStateFlow(LoadState())
    private val _state = MutableStateFlow(MyReservationsUiState())
    val state: StateFlow<MyReservationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(observeRequests(), loading) { requests, load ->
                MyReservationsUiState(
                    requests = requests,
                    isInitialLoading = !load.firstLoadDone && requests.isEmpty(),
                    isRefreshing = load.refreshing,
                    isOffline = load.offline,
                    error = load.error,
                )
            }.collect { _state.value = it }
        }
        refresh(initial = true)
        // Al abrir la campana, las respuestas quedan como vistas (el badge vuelve a 0)
        viewModelScope.launch { markUpdatesSeen() }
    }

    fun onEvent(event: MyReservationsEvent) {
        when (event) {
            MyReservationsEvent.Refresh -> refresh(initial = false)
        }
    }

    private fun refresh(initial: Boolean) {
        if (loading.value.refreshing) return
        loading.update { it.copy(refreshing = !initial, error = null) }
        viewModelScope.launch {
            val result = refreshRequests()
            val failure = (result as? AppResult.Failure)?.error
            loading.value = LoadState(
                firstLoadDone = true,
                refreshing = false,
                offline = failure == AppError.NoConnection,
                error = failure?.takeIf { it != AppError.NoConnection },
            )
        }
    }
}
