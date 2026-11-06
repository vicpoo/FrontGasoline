package org.vicpoo.gasolineproject.ui.vehicles.detail

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.vehicles.model.Vehicle

data class VehicleDetailUiState(
    val vehicle: Vehicle? = null,
    val isLoading: Boolean = true,
    /** RBAC */
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    /** RBAC (solicitud.crear) + el carro debe estar disponible. */
    val canRequest: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isDeleting: Boolean = false,
    val error: AppError? = null,
    // ---- Solicitud de reserva ----
    val showRequestDialog: Boolean = false,
    val requestReason: String = "",
    val requestReasonError: ValidationReason? = null,
    val isSendingRequest: Boolean = false,
    val requestError: AppError? = null,
    val requestSent: Boolean = false,
    /** Sin conexión: la solicitud quedó guardada y se enviará sola. */
    val requestQueued: Boolean = false,
)
