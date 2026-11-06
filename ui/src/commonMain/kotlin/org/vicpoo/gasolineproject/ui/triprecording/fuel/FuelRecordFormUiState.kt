package org.vicpoo.gasolineproject.ui.triprecording.fuel

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.triprecording.CostCenter
import org.vicpoo.gasolineproject.domain.triprecording.FuelField
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome

data class FuelRecordFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val amount: String = "",
    val liters: String = "",
    val costCenters: List<CostCenter> = emptyList(),
    val selectedCostCenterId: String? = null,
    /** Lo que se muestra en la vista previa (bytes nuevos, archivo local o URL). */
    val receiptPreview: Any? = null,
    val newReceipt: PickedFile? = null,
    val hasCurrentReceipt: Boolean = false,
    val tankCapacityText: String? = null,
    val errors: Map<FuelField, ValidationReason> = emptyMap(),
    val showReceiptOptions: Boolean = false,
    val isSaving: Boolean = false,
    /** Registro guardado (para "Reintentar"). */
    val savedRecordId: String? = null,
    val outcome: SendOutcome? = null,
    val failure: AppError? = null,
)
