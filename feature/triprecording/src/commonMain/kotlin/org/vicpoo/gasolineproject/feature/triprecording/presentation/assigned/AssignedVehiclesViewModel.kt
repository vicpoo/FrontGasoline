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
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.data.LastRefreshStore
import org.vicpoo.gasolineproject.core.network.ConnectivityMonitor
import org.vicpoo.gasolineproject.core.network.reconnections
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicle
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveReleaseBlocksUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RecordedTrip
import org.vicpoo.gasolineproject.feature.triprecording.domain.RefreshAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RefreshCostCentersUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ReleaseBlock
import org.vicpoo.gasolineproject.feature.triprecording.domain.ReleaseVehicleUseCase
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
    // ---- Dejar carro ----
    /** Carros que todavía no se pueden dejar y por qué (viaje en curso o sin enviar). */
    val releaseBlocks: Map<String, ReleaseBlock> = emptyMap(),
    val releaseTarget: AssignedVehicle? = null,
    val releaseNotes: String = "",
    val releaseNotesError: ValidationReason? = null,
    val isReleasing: Boolean = false,
    /** Se dejó con éxito (diálogo de resultado). */
    val released: AssignedVehicle? = null,
    val releaseError: AppError? = null,
) {
    /** null si se puede dejar; si no, el motivo que se muestra bajo el botón. */
    fun releaseBlockOf(vehicle: AssignedVehicle): ReleaseBlock? = releaseBlocks[vehicle.reservationId]
}

sealed interface AssignedVehiclesEvent {
    data object Refresh : AssignedVehiclesEvent
    data class ReleaseClicked(val vehicle: AssignedVehicle) : AssignedVehiclesEvent
    data class ReleaseNotesChanged(val value: String) : AssignedVehiclesEvent
    data object ReleaseConfirmed : AssignedVehiclesEvent
    data object DialogDismissed : AssignedVehiclesEvent
}

/** Lo que pasa en el diálogo de "Dejar carro" (se combina con el resto de la pantalla). */
private data class ReleaseUi(
    val target: AssignedVehicle? = null,
    val notes: String = "",
    val notesError: ValidationReason? = null,
    val sending: Boolean = false,
    val released: AssignedVehicle? = null,
    val error: AppError? = null,
)

/**
 * Pestaña "Registrar": carros con reserva aprobada + acceso al viaje en curso.
 * "Dejar carro": el empleado lo libera para que otro lo pueda pedir. No se puede con un viaje
 * en curso / en pausa / esperando el kilometraje, ni con un viaje guardado sin enviar; requiere red.
 */
class AssignedVehiclesViewModel(
    observeVehicles: ObserveAssignedVehiclesUseCase,
    observeTracking: ObserveTrackingUseCase,
    observeReleaseBlocks: ObserveReleaseBlocksUseCase,
    private val releaseVehicle: ReleaseVehicleUseCase,
    private val refreshVehicles: RefreshAssignedVehiclesUseCase,
    private val refreshCostCenters: RefreshCostCentersUseCase,
    private val syncPendingTrips: SyncPendingTripsUseCase,
    /** Se recarga sola al volver la red y marca "sin conexión" en cuanto se pierde. */
    private val connectivity: ConnectivityMonitor,
    private val lastRefresh: LastRefreshStore,
) : ViewModel() {

    private val loading = MutableStateFlow(LoadState())
    private val release = MutableStateFlow(ReleaseUi())
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
            }.combine(observeReleaseBlocks()) { base, blocks -> base.copy(releaseBlocks = blocks) }
                .combine(release) { base, ui ->
                    base.copy(
                        releaseTarget = ui.target,
                        releaseNotes = ui.notes,
                        releaseNotesError = ui.notesError,
                        isReleasing = ui.sending,
                        released = ui.released,
                        releaseError = ui.error,
                    )
                }
                .collect { _state.value = it }
        }
        refresh(initial = true)
        syncPendingTrips() // si quedó algo pendiente de otro día, se manda ahora
        viewModelScope.launch { connectivity.reconnections().collect { refresh(initial = true) } }
    }

    fun onEvent(event: AssignedVehiclesEvent) {
        when (event) {
            AssignedVehiclesEvent.Refresh -> refresh(initial = false)
            is AssignedVehiclesEvent.ReleaseClicked -> release.value = ReleaseUi(target = event.vehicle)
            is AssignedVehiclesEvent.ReleaseNotesChanged -> release.update { it.copy(notes = event.value, notesError = null) }
            AssignedVehiclesEvent.ReleaseConfirmed -> releaseTarget()
            AssignedVehiclesEvent.DialogDismissed -> release.update { if (it.sending) it else ReleaseUi() }
        }
    }

    private fun releaseTarget() {
        val current = release.value
        val vehicle = current.target ?: return
        if (current.sending) return
        release.update { it.copy(sending = true) }
        viewModelScope.launch {
            when (val result = releaseVehicle(vehicle.reservationId, current.notes)) {
                is AppResult.Success -> release.value = ReleaseUi(released = vehicle)
                is AppResult.Failure -> release.update {
                    val validation = (result.error as? AppError.Validation)?.reason
                    if (validation != null) {
                        it.copy(sending = false, notesError = validation)
                    } else {
                        it.copy(sending = false, target = null, error = result.error)
                    }
                }
            }
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
