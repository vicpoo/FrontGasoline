package org.vicpoo.gasolineproject.ui.triprecording.start

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicle
import org.vicpoo.gasolineproject.domain.triprecording.CostCenter

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
