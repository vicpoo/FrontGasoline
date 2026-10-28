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
import org.vicpoo.gasolineproject.core.data.LastRefreshStore
import org.vicpoo.gasolineproject.core.network.ConnectivityMonitor
import org.vicpoo.gasolineproject.core.network.reconnections
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
    /** Cuándo se trajo de la API la copia que se ve (para "Actualizado hace X"). */
    val lastUpdatedAtMillis: Long? = null,
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
    /** Se recarga sola al volver la red y marca "sin conexión" en cuanto se pierde. */
    private val connectivity: ConnectivityMonitor,
    private val lastRefresh: LastRefreshStore,
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
            combine(
                observeVehicles(),
                observeTracking(),
                loading,
                connectivity.isOnline,
                lastRefresh.observe(REFRESH_KEY),
            ) { vehicles, tracking, load, online, lastUpdated ->
                AssignedVehiclesUiState(
                    vehicles = vehicles,
                    activeTrip = tracking.trip,
                    isInitialLoading = !load.firstLoadDone && vehicles.isEmpty(),
                    isRefreshing = load.refreshing,
                    isOffline = load.offline || !online,
                    error = load.error,
                    lastUpdatedAtMillis = lastUpdated,
                )
            }.collect { _state.value = it }
        }
        refresh(initial = true)
        syncPendingTrips() // si quedó algo pendiente de otro día, se manda ahora
        viewModelScope.launch { connectivity.reconnections().collect { refresh(initial = true) } }
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
            if (result is AppResult.Success) lastRefresh.markNow(REFRESH_KEY)
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

    private companion object {
        /** Llave de "Actualizado hace X" de esta lista. */
        const val REFRESH_KEY = "triprecording.assigned"
    }
}
