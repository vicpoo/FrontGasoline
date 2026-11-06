package org.vicpoo.gasolineproject.ui.reservations.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.LastRefreshStore
import org.vicpoo.gasolineproject.domain.connectivity.ConnectivityMonitor
import org.vicpoo.gasolineproject.domain.connectivity.reconnections
import org.vicpoo.gasolineproject.domain.push.PushEvents
import org.vicpoo.gasolineproject.domain.push.PushType
import org.vicpoo.gasolineproject.domain.reservations.MarkReservationUpdatesSeenUseCase
import org.vicpoo.gasolineproject.domain.reservations.ObserveMyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.RefreshMyReservationsUseCase

/** Campana de Inicio: las solicitudes que envió el usuario y su estado (solo para ver cómo van). */
class MyReservationsViewModel(
    observeRequests: ObserveMyReservationsUseCase,
    private val refreshRequests: RefreshMyReservationsUseCase,
    private val markUpdatesSeen: MarkReservationUpdatesSeenUseCase,
    pushEvents: PushEvents,
    /** Se recarga sola al volver la red y marca "sin conexión" en cuanto se pierde. */
    private val connectivity: ConnectivityMonitor,
    private val lastRefresh: LastRefreshStore,
) : ViewModel() {

    private data class LoadState(
        val firstLoadDone: Boolean = false,
        val refreshing: Boolean = false,
        val offline: Boolean = false,
        val error: AppError? = null,
    )

    private val loading = MutableStateFlow(LoadState())
    private val filter = MutableStateFlow(MyReservationsFilter.ALL)
    private val _state = MutableStateFlow(MyReservationsUiState())
    val state: StateFlow<MyReservationsUiState> = _state.asStateFlow()

    init {
        // Respondieron una de mis solicitudes con la app abierta: se actualiza sin el indicador
        viewModelScope.launch {
            pushEvents.received.collect { if (it.type == PushType.REQUEST_RESULT) refresh(initial = true) }
        }
        viewModelScope.launch {
            combine(
                observeRequests(),
                loading,
                connectivity.isOnline,
                lastRefresh.observe(REFRESH_KEY),
                filter,
            ) { requests, load, online, lastUpdated, selected ->
                MyReservationsUiState(
                    requests = requests,
                    filter = selected,
                    counts = MyReservationsGrouping.counts(requests),
                    groups = MyReservationsGrouping.group(requests, selected),
                    isInitialLoading = !load.firstLoadDone && requests.isEmpty(),
                    isRefreshing = load.refreshing,
                    isOffline = load.offline || !online,
                    error = load.error,
                    lastUpdatedAtMillis = lastUpdated,
                )
            }.collect { _state.value = it }
        }
        refresh(initial = true)
        viewModelScope.launch { connectivity.reconnections().collect { refresh(initial = true) } }
        // Al abrir la campana, las respuestas quedan como vistas (el badge vuelve a 0)
        viewModelScope.launch { markUpdatesSeen() }
    }

    fun onEvent(event: MyReservationsEvent) {
        when (event) {
            MyReservationsEvent.Refresh -> refresh(initial = false)
            is MyReservationsEvent.FilterSelected -> filter.value = event.filter
        }
    }

    /** Al entrar a la pantalla y cada cierto tiempo: actualiza sin mostrar el indicador. */
    fun refreshSilently() = refresh(initial = true)

    private fun refresh(initial: Boolean) {
        if (loading.value.refreshing) return
        loading.update { it.copy(refreshing = !initial, error = null) }
        viewModelScope.launch {
            val result = refreshRequests()
            val failure = (result as? AppResult.Failure)?.error
            if (failure == null) lastRefresh.markNow(REFRESH_KEY)
            loading.value = LoadState(
                firstLoadDone = true,
                refreshing = false,
                offline = failure == AppError.NoConnection,
                error = failure?.takeIf { it != AppError.NoConnection },
            )
        }
    }

    private companion object {
        /** Llave de "Actualizado hace X" de esta lista. */
        const val REFRESH_KEY = "reservations.mine"
    }
}
