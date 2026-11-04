package org.vicpoo.gasolineproject.feature.reservations.presentation.assignmentdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.network.ConnectivityMonitor
import org.vicpoo.gasolineproject.feature.reservations.domain.AssignmentDetail
import org.vicpoo.gasolineproject.feature.reservations.domain.AssignmentKey
import org.vicpoo.gasolineproject.feature.reservations.domain.GetAssignmentsUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.ObserveAssignmentDetailUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.UnlinkAssignmentUseCase

/** A quién se le va a quitar qué carro (para el diálogo y el mensaje de éxito). */
data class UnlinkTarget(
    val reservationId: String,
    val vehicleTitle: String,
    val plates: String,
    val employeeName: String,
)

data class AssignmentDetailUiState(
    val detail: AssignmentDetail? = null,
    /** Todavía no se sabe si existe (primera carga). */
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val error: AppError? = null,
    // ---- Desvincular ----
    val unlinkTarget: UnlinkTarget? = null,
    val unlinkReason: String = "",
    val unlinkReasonError: ValidationReason? = null,
    val isSending: Boolean = false,
    /** Se desvinculó con éxito (diálogo de resultado). */
    val unlinked: UnlinkTarget? = null,
    val unlinkError: AppError? = null,
    /** Ya no queda nada que mostrar (se desvinculó el último carro): regresar a la lista. */
    val finished: Boolean = false,
) {
    /** Desvincular necesita red. */
    val canUnlink: Boolean get() = !isOffline && !isSending
}

sealed interface AssignmentDetailEvent {
    data object Retry : AssignmentDetailEvent
    data class UnlinkClicked(val target: UnlinkTarget) : AssignmentDetailEvent
    data class UnlinkReasonChanged(val value: String) : AssignmentDetailEvent
    data object UnlinkConfirmed : AssignmentDetailEvent
    data object DialogDismissed : AssignmentDetailEvent
}

/**
 * Detalle de Asignaciones (como el de Solicitudes con Aceptar / Rechazar):
 * - Desde "Carros": el carro y quién lo tiene.
 * - Desde "Empleados": el empleado y todos sus carros.
 * Cada carro tiene "Desvincular" (motivo obligatorio; no se puede con un viaje en curso).
 * Abre al instante con lo último que cargó la lista y se actualiza de la API.
 */
class AssignmentDetailViewModel(
    keyValue: String,
    observeDetail: ObserveAssignmentDetailUseCase,
    private val getAssignments: GetAssignmentsUseCase,
    private val unlinkAssignment: UnlinkAssignmentUseCase,
    connectivity: ConnectivityMonitor,
) : ViewModel() {

    private val key: AssignmentKey? = AssignmentKey.parse(keyValue)
    private val ui = MutableStateFlow(AssignmentDetailUiState())

    /** Lo que hay ahora en la API (null = no existe o ya se desvinculó). */
    private val fresh: StateFlow<AssignmentDetail?> =
        (key?.let(observeDetail::invoke) ?: flowOf(null)).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<AssignmentDetailUiState> = combine(ui, fresh, connectivity.isOnline) { current, detail, online ->
        current.copy(
            // Mientras se muestra el éxito se conserva lo último (el carro ya no está en la API)
            detail = detail ?: current.detail.takeIf { current.unlinked != null },
            isOffline = current.isOffline || !online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssignmentDetailUiState())

    init {
        load()
    }

    fun onEvent(event: AssignmentDetailEvent) {
        when (event) {
            AssignmentDetailEvent.Retry -> load()
            is AssignmentDetailEvent.UnlinkClicked -> ui.update {
                it.copy(unlinkTarget = event.target, unlinkReason = "", unlinkReasonError = null)
            }
            is AssignmentDetailEvent.UnlinkReasonChanged -> ui.update {
                it.copy(unlinkReason = event.value, unlinkReasonError = null)
            }
            AssignmentDetailEvent.UnlinkConfirmed -> unlink()
            AssignmentDetailEvent.DialogDismissed -> ui.update {
                when {
                    it.isSending -> it
                    // Después del éxito: si ya no le quedan carros, se regresa a la lista
                    it.unlinked != null -> it.copy(unlinked = null, detail = null, finished = fresh.value == null)
                    else -> it.copy(unlinkTarget = null, unlinkError = null)
                }
            }
        }
    }

    private fun load() {
        ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = getAssignments()
            ui.update {
                it.copy(
                    isLoading = false,
                    isOffline = result is AppResult.Failure && result.error == AppError.NoConnection,
                    error = (result as? AppResult.Failure)?.error?.takeIf { error -> error != AppError.NoConnection },
                )
            }
        }
    }

    private fun unlink() {
        val target = ui.value.unlinkTarget ?: return
        if (ui.value.isSending) return
        ui.update { it.copy(isSending = true) }
        viewModelScope.launch {
            when (val result = unlinkAssignment(target.reservationId, ui.value.unlinkReason)) {
                is AppResult.Success -> {
                    // Se conserva lo que se veía hasta cerrar el mensaje de éxito
                    ui.update { it.copy(isSending = false, unlinkTarget = null, unlinked = target, detail = state.value.detail) }
                    getAssignments()
                }
                is AppResult.Failure -> {
                    val validation = (result.error as? AppError.Validation)?.reason
                    ui.update {
                        if (validation != null) {
                            it.copy(isSending = false, unlinkReasonError = validation)
                        } else {
                            it.copy(isSending = false, unlinkTarget = null, unlinkError = result.error)
                        }
                    }
                    // 409 (viaje en curso desde otro celular, o ya se había liberado): se recarga para reflejarlo
                    if (validation == null) getAssignments()
                }
            }
        }
    }
}
