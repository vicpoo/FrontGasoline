package org.vicpoo.gasolineproject.feature.triprecording.presentation.assigned

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
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicle
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RecordedTrip
import org.vicpoo.gasolineproject.feature.triprecording.domain.RefreshAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RefreshCostCentersUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.SyncPendingTripsUseCase

data class AssignedVehiclesUiState(
    val vehicles: List<AssignedVehicle> = emptyList(),
    val activeTrip: RecordedTrip? = null,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: AppError? = null,
)

sealed interface AssignedVehiclesEvent {
    data object Refresh : AssignedVehiclesEvent
}

/** Pestaña "Registrar": carros con reserva aprobada + acceso al viaje en curso. */
class AssignedVehiclesViewModel(
    observeVehicles: ObserveAssignedVehiclesUseCase,
    observeTracking: ObserveTrackingUseCase,
    private val refreshVehicles: RefreshAssignedVehiclesUseCase,
    private val refreshCostCenters: RefreshCostCentersUseCase,
    private val syncPendingTrips: SyncPendingTripsUseCase,
) : ViewModel() {

    private val loading = MutableStateFlow(LoadState())
    private val _state = MutableStateFlow(AssignedVehiclesUiState())
    val state: StateFlow<AssignedVehiclesUiState> = _state.asStateFlow()

    private data class LoadState(
        val firstLoadDone: Boolean = false,
        val refreshing: Boolean = false,
        val offline: Boolean = false,
        val error: AppError? = null,
    )

    init {
        viewModelScope.launch {
            combine(observeVehicles(), observeTracking(), loading) { vehicles, tracking, load ->
                AssignedVehiclesUiState(
                    vehicles = vehicles,
                    activeTrip = tracking.trip,
                    isInitialLoading = !load.firstLoadDone && vehicles.isEmpty(),
                    isRefreshing = load.refreshing,
                    isOffline = load.offline,
                    error = load.error,
                )
            }.collect { _state.value = it }
        }
        refresh(initial = true)
        syncPendingTrips() // si quedó algo pendiente de otro día, se manda ahora
    }

    fun onEvent(event: AssignedVehiclesEvent) {
        when (event) {
            AssignedVehiclesEvent.Refresh -> refresh(initial = false)
        }
    }

    private fun refresh(initial: Boolean) {
        if (loading.value.refreshing) return
        loading.update { it.copy(refreshing = !initial, error = null) }
        viewModelScope.launch {
            // Los centros de costo se guardan ya para poder iniciar un viaje sin red
            launch { refreshCostCenters() }
            val result = refreshVehicles()
            loading.update {
                LoadState(
                    firstLoadDone = true,
                    refreshing = false,
                    offline = result is AppResult.Failure && result.error == AppError.NoConnection,
                    error = (result as? AppResult.Failure)?.error?.takeIf { e -> e != AppError.NoConnection },
                )
            }
        }
    }
}
