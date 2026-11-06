package org.vicpoo.gasolineproject.ui.reservations.assignmentdetail

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.reservations.AssignmentDetail

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
