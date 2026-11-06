package org.vicpoo.gasolineproject.ui.triprecording.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.rbac.PermissionChecker
import org.vicpoo.gasolineproject.domain.rbac.Privileges
import org.vicpoo.gasolineproject.domain.triprecording.DeleteFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.LocationPermissionChangedUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveFuelRecordsUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.domain.triprecording.PauseTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ResumeTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.StopTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase

/** Figma "viaje en curso": mapa, duración / distancia / fecha, pausa, terminar y recargas. */
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveTripViewModel(
    observeTracking: ObserveTrackingUseCase,
    observeFuelRecords: ObserveFuelRecordsUseCase,
    permissionChecker: PermissionChecker,
    private val pauseTrip: PauseTripUseCase,
    private val resumeTrip: ResumeTripUseCase,
    private val stopTrip: StopTripUseCase,
    private val deleteFuelRecord: DeleteFuelRecordUseCase,
    private val locationPermissionChanged: LocationPermissionChangedUseCase,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(ActiveTripUiState())
    val state: StateFlow<ActiveTripUiState> = _state.asStateFlow()

    init {
        val tracking = observeTracking()
        val fuel = tracking
            .map { it.trip?.localId }
            .distinctUntilChanged()
            .flatMapLatest { tripId -> if (tripId == null) flowOf(emptyList()) else observeFuelRecords(tripId) }
        val permissions = combine(
            permissionChecker.observe(Privileges.CARGA_REGISTRAR),
            permissionChecker.observe(Privileges.CARGA_EDITAR),
            permissionChecker.observe(Privileges.CARGA_ELIMINAR),
        ) { add, edit, delete -> FuelPermissions(add, edit, delete) }

        viewModelScope.launch {
            combine(tracking, fuel, permissions) { session, records, fuelPermissions ->
                Triple(session, records, fuelPermissions)
            }.collect { (session, records, fuelPermissions) ->
                _state.update {
                    it.copy(
                        session = session,
                        fuelRecords = records,
                        fuel = fuelPermissions,
                        elapsedMillis = session.trip?.activeMillis(clock.nowMillis()) ?: 0L,
                        // Si la app se cerró en "Registrar Viaje", se regresa ahí
                        navigateToFinish = it.navigateToFinish || (!it.isReady && session.trip?.phase == TripPhase.STOPPED),
                        isReady = true,
                    )
                }
            }
        }

        // Reloj de "Duración" (no avanza en pausa)
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                val trip = _state.value.session.trip ?: continue
                _state.update { it.copy(elapsedMillis = trip.activeMillis(clock.nowMillis())) }
            }
        }
    }

    fun onEvent(event: ActiveTripEvent) {
        when (event) {
            ActiveTripEvent.PauseClicked -> changePause { pauseTrip() }
            ActiveTripEvent.ResumeClicked -> changePause { resumeTrip() }
            ActiveTripEvent.FinishClicked -> _state.update { it.copy(showFinishConfirm = true) }
            ActiveTripEvent.FinishDismissed -> _state.update { it.copy(showFinishConfirm = false) }
            ActiveTripEvent.FinishConfirmed -> stop()
            ActiveTripEvent.NavigationHandled -> _state.update { it.copy(navigateToFinish = false) }
            ActiveTripEvent.ErrorDismissed -> _state.update { it.copy(error = null) }
            is ActiveTripEvent.DeleteClicked -> _state.update { it.copy(deleteCandidate = event.record) }
            ActiveTripEvent.DeleteDismissed -> _state.update { it.copy(deleteCandidate = null) }
            ActiveTripEvent.DeleteConfirmed -> delete()
            ActiveTripEvent.PermissionChanged -> locationPermissionChanged()
        }
    }

    private fun changePause(action: suspend () -> AppResult<*>) {
        if (_state.value.isChangingPause) return
        _state.update { it.copy(isChangingPause = true) }
        viewModelScope.launch {
            val result = action()
            _state.update { it.copy(isChangingPause = false, error = (result as? AppResult.Failure)?.error) }
        }
    }

    /** "¿Está seguro de terminar con el viaje?" → Sí: se detiene el GPS y se abre "Registrar Viaje". */
    private fun stop() {
        if (_state.value.isStopping) return
        _state.update { it.copy(isStopping = true) }
        viewModelScope.launch {
            val result = stopTrip()
            _state.update {
                it.copy(
                    isStopping = false,
                    showFinishConfirm = false,
                    navigateToFinish = result is AppResult.Success,
                    error = (result as? AppResult.Failure)?.error,
                )
            }
        }
    }

    private fun delete() {
        val record = _state.value.deleteCandidate ?: return
        _state.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            deleteFuelRecord(record.localId)
            _state.update { it.copy(isDeleting = false, deleteCandidate = null) }
        }
    }
}
