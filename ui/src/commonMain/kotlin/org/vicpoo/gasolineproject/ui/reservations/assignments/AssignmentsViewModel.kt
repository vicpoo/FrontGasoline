package org.vicpoo.gasolineproject.ui.reservations.assignments

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
import org.vicpoo.gasolineproject.domain.connectivity.ConnectivityMonitor
import org.vicpoo.gasolineproject.domain.connectivity.reconnections
import org.vicpoo.gasolineproject.domain.reservations.GetAssignmentsUseCase

/**
 * Pestaña Asignaciones (asignacion.gestionar): solo los carros vinculados a un empleado, vistos
 * por carro o por empleado, en tarjetas simples. Al tocar una se abre el detalle, donde está
 * "Desvincular" (ver AssignmentDetailViewModel), igual que Aceptar / Rechazar en Solicitudes.
 * Requiere red: sin conexión se muestra lo último que se cargó y el aviso.
 */
class AssignmentsViewModel(
    private val getAssignments: GetAssignmentsUseCase,
    private val connectivity: ConnectivityMonitor,
) : ViewModel() {

    private val ui = MutableStateFlow(AssignmentsUiState())
    private val _state = MutableStateFlow(AssignmentsUiState())
    val state: StateFlow<AssignmentsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(ui, connectivity.isOnline) { current, online ->
                current.copy(isOffline = current.isOffline || !online)
            }.collect { _state.value = it }
        }
        load(silent = true)
        viewModelScope.launch { connectivity.reconnections().collect { load(silent = true) } }
    }

    fun onEvent(event: AssignmentsEvent) {
        when (event) {
            AssignmentsEvent.Refresh -> load(silent = false)
            is AssignmentsEvent.SegmentToggled -> ui.update {
                it.copy(segment = if (it.segment == event.segment) null else event.segment)
            }
            is AssignmentsEvent.QueryChanged -> ui.update { it.copy(query = event.value) }
            AssignmentsEvent.SearchOpened -> ui.update { it.copy(isSearchOpen = true) }
            AssignmentsEvent.SearchDismissed -> ui.update { it.copy(isSearchOpen = false) }
            AssignmentsEvent.SearchCleared -> ui.update { it.copy(query = "", segment = null) }
        }
    }

    /** Al entrar a la pantalla (o volver del detalle): actualiza sin mostrar el indicador. */
    fun refreshSilently() = load(silent = true)

    private fun load(silent: Boolean) {
        if (ui.value.isRefreshing) return
        ui.update { it.copy(isRefreshing = !silent, error = null) }
        viewModelScope.launch {
            when (val result = getAssignments()) {
                is AppResult.Success -> ui.update {
                    it.copy(overview = result.data, isInitialLoading = false, isRefreshing = false, isOffline = false)
                }
                is AppResult.Failure -> ui.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        isOffline = result.error == AppError.NoConnection,
                        error = result.error.takeIf { error -> error != AppError.NoConnection },
                    )
                }
            }
        }
    }
}
