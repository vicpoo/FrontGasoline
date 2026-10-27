package org.vicpoo.gasolineproject.feature.trips.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.feature.trips.domain.ObserveTripsUseCase
import org.vicpoo.gasolineproject.feature.trips.domain.RefreshTripsUseCase
import org.vicpoo.gasolineproject.feature.trips.domain.Trip

data class TripHistoryUiState(
    val trips: List<Trip> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: AppError? = null,
)

class TripHistoryViewModel(
    observeTrips: ObserveTripsUseCase,
    private val refreshTrips: RefreshTripsUseCase,
) : ViewModel() {

    private data class Status(
        val loadedOnce: Boolean = false,
        /** Hay una petición en curso (visible o en segundo plano). */
        val isLoading: Boolean = false,
        /** Solo true cuando el usuario jaló para recargar: es lo único que muestra el indicador. */
        val isRefreshing: Boolean = false,
        val isOffline: Boolean = false,
        val error: AppError? = null,
    )

    private val status = MutableStateFlow(Status())

    val state: StateFlow<TripHistoryUiState> = combine(observeTrips(), status) { trips, s ->
        TripHistoryUiState(
            trips = trips,
            isInitialLoading = trips.isEmpty() && !s.loadedOnce,
            isRefreshing = s.isRefreshing,
            isOffline = s.isOffline,
            error = s.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripHistoryUiState())

    init {
        refreshSilently()
    }

    /** El usuario jaló para recargar: se muestra el indicador. */
    fun refresh() = load(showIndicator = true)

    /** Al abrir o volver a la pestaña: se actualiza sin mostrar el indicador. */
    fun refreshSilently() = load(showIndicator = false)

    private fun load(showIndicator: Boolean) {
        if (status.value.isLoading) {
            // Si ya hay una carga silenciosa y el usuario jala, se muestra el indicador hasta que termine
            if (showIndicator) status.update { it.copy(isRefreshing = true) }
            return
        }
        viewModelScope.launch {
            status.update { it.copy(isLoading = true, isRefreshing = showIndicator) }
            val failure = (refreshTrips() as? AppResult.Failure)?.error
            status.value = Status(
                loadedOnce = true,
                isOffline = failure == AppError.NoConnection,
                error = failure?.takeIf { it != AppError.NoConnection },
            )
        }
    }
}
