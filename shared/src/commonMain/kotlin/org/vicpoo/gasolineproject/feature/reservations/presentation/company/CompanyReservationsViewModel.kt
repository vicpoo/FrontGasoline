package org.vicpoo.gasolineproject.feature.reservations.presentation.company

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
import org.vicpoo.gasolineproject.feature.reservations.domain.ObserveCompanyReservationsUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.RefreshCompanyReservationsUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRequest
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationStatus

/** Filtro de la lista: primero lo que falta por decidir. */
enum class CompanyReservationsFilter { PENDING, ALL }

data class CompanyReservationsUiState(
    val requests: List<ReservationRequest> = emptyList(),
    val filter: CompanyReservationsFilter = CompanyReservationsFilter.PENDING,
    val pendingCount: Int = 0,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: AppError? = null,
)

sealed interface CompanyReservationsEvent {
    data object Refresh : CompanyReservationsEvent
    data class FilterChanged(val filter: CompanyReservationsFilter) : CompanyReservationsEvent
}

/**
 * Pestaña llave: las solicitudes de toda la empresa ("Solicitud de {empleado}").
 * Aceptar y rechazar se hacen en el detalle (al tocar la tarjeta).
 */
class CompanyReservationsViewModel(
    observeRequests: ObserveCompanyReservationsUseCase,
    private val refreshRequests: RefreshCompanyReservationsUseCase,
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
        viewModelScope.launch {
            combine(observeRequests(), loading, filter) { requests, load, selected ->
                CompanyReservationsUiState(
                    requests = when (selected) {
                        CompanyReservationsFilter.PENDING -> requests.filter { it.status == ReservationStatus.PENDING }
                        CompanyReservationsFilter.ALL -> requests
                    },
                    filter = selected,
                    pendingCount = requests.count { it.status == ReservationStatus.PENDING },
                    isInitialLoading = !load.firstLoadDone && requests.isEmpty(),
                    isRefreshing = load.refreshing,
                    isOffline = load.offline,
                    error = load.error,
                )
            }.collect { _state.value = it }
        }
        refresh(initial = true)
    }

    fun onEvent(event: CompanyReservationsEvent) {
        when (event) {
            CompanyReservationsEvent.Refresh -> refresh(initial = false)
            is CompanyReservationsEvent.FilterChanged -> filter.value = event.filter
        }
    }

    private fun refresh(initial: Boolean) {
        if (loading.value.refreshing) return
        loading.update { it.copy(refreshing = !initial, error = null) }
        viewModelScope.launch {
            val failure = (refreshRequests() as? AppResult.Failure)?.error
            loading.value = LoadState(
                firstLoadDone = true,
                refreshing = false,
                offline = failure == AppError.NoConnection,
                error = failure?.takeIf { it != AppError.NoConnection },
            )
        }
    }
}
