package org.vicpoo.gasolineproject.ui.reservations.company

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
import org.vicpoo.gasolineproject.domain.reservations.ObserveCompanyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.RefreshCompanyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.ReservationStatus

/**
 * Pestaña llave: las solicitudes de toda la empresa ("Solicitud de {empleado}").
 * Aceptar y rechazar se hacen en el detalle (al tocar la tarjeta).
 */
class CompanyReservationsViewModel(
    observeRequests: ObserveCompanyReservationsUseCase,
    private val refreshRequests: RefreshCompanyReservationsUseCase,
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
    private val filter = MutableStateFlow(CompanyReservationsFilter.PENDING)
    private val _state = MutableStateFlow(CompanyReservationsUiState())
    val state: StateFlow<CompanyReservationsUiState> = _state.asStateFlow()

    init {
        // Llegó una solicitud nueva con la app abierta: se actualiza la lista sin el indicador
        viewModelScope.launch {
            pushEvents.received.collect { if (it.type == PushType.NEW_REQUEST) refresh(initial = true) }
        }
        viewModelScope.launch {
            combine(
                observeRequests(),
                loading,
                filter,
                connectivity.isOnline,
                lastRefresh.observe(REFRESH_KEY),
            ) { requests, load, selected, online, lastUpdated ->
                CompanyReservationsUiState(
                    requests = when (selected) {
                        CompanyReservationsFilter.PENDING -> requests.filter { it.status == ReservationStatus.PENDING }
                        CompanyReservationsFilter.ALL -> requests
                    },
                    filter = selected,
                    pendingCount = requests.count { it.status == ReservationStatus.PENDING },
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
    }

    fun onEvent(event: CompanyReservationsEvent) {
        when (event) {
            CompanyReservationsEvent.Refresh -> refresh(initial = false)
            is CompanyReservationsEvent.FilterChanged -> filter.value = event.filter
        }
    }

    /** Al entrar a la pantalla y cada cierto tiempo: actualiza sin mostrar el indicador. */
    fun refreshSilently() = refresh(initial = true)

    private fun refresh(initial: Boolean) {
        if (loading.value.refreshing) return
        loading.update { it.copy(refreshing = !initial, error = null) }
        viewModelScope.launch {
            val failure = (refreshRequests() as? AppResult.Failure)?.error
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
        const val REFRESH_KEY = "reservations.company"
    }
}
