package org.vicpoo.gasolineproject.feature.triprecording.presentation.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicle
import org.vicpoo.gasolineproject.feature.triprecording.domain.CheckLocationPermissionUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenter
import org.vicpoo.gasolineproject.feature.triprecording.domain.GetAssignedVehicleUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.LocationPermissionChangedUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveCostCentersUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RefreshCostCentersUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.StartTripUseCase

data class StartTripArgs(val reservationId: String)

data class StartTripUiState(
    val isLoading: Boolean = true,
    val vehicle: AssignedVehicle? = null,
    val costCenters: List<CostCenter> = emptyList(),
    val selectedCostCenterId: String? = null,
    val isStarting: Boolean = false,
    val showCostCenterError: Boolean = false,
    val permissionDenied: Boolean = false,
    val error: AppError? = null,
)

sealed interface StartTripEvent {
    data class CostCenterSelected(val id: String) : StartTripEvent
    data object StartClicked : StartTripEvent
    data class PermissionResult(val granted: Boolean) : StartTripEvent
}

sealed interface StartTripEffect {
    data object RequestLocationPermission : StartTripEffect
    data object TripStarted : StartTripEffect
}

/** Figma "Iniciar Viaje": carro + centro de costos + "Comenzar Viaje". */
class StartTripViewModel(
    private val args: StartTripArgs,
    private val getVehicle: GetAssignedVehicleUseCase,
    observeCostCenters: ObserveCostCentersUseCase,
    private val refreshCostCenters: RefreshCostCentersUseCase,
    private val startTrip: StartTripUseCase,
    private val hasLocationPermission: CheckLocationPermissionUseCase,
    private val locationPermissionChanged: LocationPermissionChangedUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(StartTripUiState())
    val state: StateFlow<StartTripUiState> = _state.asStateFlow()

    private val effects = Channel<StartTripEffect>(Channel.BUFFERED)
    val effect: Flow<StartTripEffect> = effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val vehicle = getVehicle(args.reservationId)
            _state.update { it.copy(vehicle = vehicle, isLoading = false, error = if (vehicle == null) AppError.NotFound else null) }
        }
        viewModelScope.launch {
            observeCostCenters().collect { list ->
                _state.update { current ->
                    current.copy(
                        costCenters = list,
                        // Si solo hay uno se elige solo
                        selectedCostCenterId = current.selectedCostCenterId?.takeIf { id -> list.any { it.id == id } }
                            ?: list.singleOrNull()?.id,
                    )
                }
            }
        }
        viewModelScope.launch { refreshCostCenters() }
    }

    fun onEvent(event: StartTripEvent) {
        when (event) {
            is StartTripEvent.CostCenterSelected ->
                _state.update { it.copy(selectedCostCenterId = event.id, showCostCenterError = false) }
            StartTripEvent.StartClicked -> onStartClicked()
            is StartTripEvent.PermissionResult -> {
                locationPermissionChanged()
                if (event.granted) start() else _state.update { it.copy(permissionDenied = true) }
            }
        }
    }

    private fun onStartClicked() {
        val current = _state.value
        if (current.isStarting || current.vehicle == null) return
        if (current.costCenters.isNotEmpty() && current.selectedCostCenterId == null) {
            _state.update { it.copy(showCostCenterError = true) }
            return
        }
        if (!hasLocationPermission()) {
            viewModelScope.launch { effects.send(StartTripEffect.RequestLocationPermission) }
            return
        }
        start()
    }

    private fun start() {
        val current = _state.value
        val vehicle = current.vehicle ?: return
        val costCenter = current.costCenters.firstOrNull { it.id == current.selectedCostCenterId }
        _state.update { it.copy(isStarting = true, error = null, permissionDenied = false) }
        viewModelScope.launch {
            when (val result = startTrip(vehicle, costCenter, current.costCenters)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isStarting = false) }
                    effects.send(StartTripEffect.TripStarted)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(
                        isStarting = false,
                        showCostCenterError = result.error is AppError.Validation,
                        error = result.error.takeIf { e -> e !is AppError.Validation },
                    )
                }
            }
        }
    }
}
