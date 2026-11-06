package org.vicpoo.gasolineproject.ui.reservations.detail

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.reservations.ReservationDetail
import org.vicpoo.gasolineproject.domain.reservations.ReservationRequest
import org.vicpoo.gasolineproject.domain.reservations.ReservationStatus

/** Qué se le contestó a la solicitud (para el diálogo de resultado). */
enum class ReservationDecision { APPROVED, REJECTED }

data class ReservationDetailUiState(
    /** Lo guardado en el teléfono: la pantalla abre al instante y sin red. */
    val request: ReservationRequest? = null,
    /** Detalle completo de la API (motivo, licencia, carro, quién resolvió). */
    val detail: ReservationDetail? = null,
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val error: AppError? = null,
    /** RBAC: cada botón con su privilegio. */
    val canApprove: Boolean = false,
    val canReject: Boolean = false,
    // ---- Aceptar ----
    val showApproveConfirm: Boolean = false,
    // ---- Rechazar ----
    val showRejectDialog: Boolean = false,
    val rejectReason: String = "",
    val rejectReasonError: ValidationReason? = null,
    // ---- Envío ----
    val isSending: Boolean = false,
    val decision: ReservationDecision? = null,
    val decisionError: AppError? = null,
    // ---- Licencia a pantalla completa ----
    val showLicense: Boolean = false,
) {
    /** La que se muestra: la de la API si ya llegó; si no, la guardada. */
    val current: ReservationRequest? get() = detail?.request ?: request

    /** Aceptar / Rechazar solo en pendientes, con red y con su privilegio. */
    val canDecide: Boolean get() = current?.status == ReservationStatus.PENDING && !isOffline && (canApprove || canReject)
}
