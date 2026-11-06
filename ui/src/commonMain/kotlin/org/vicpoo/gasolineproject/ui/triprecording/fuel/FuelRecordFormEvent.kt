package org.vicpoo.gasolineproject.ui.triprecording.fuel

import org.vicpoo.gasolineproject.domain.files.PickedFile

sealed interface FuelRecordFormEvent {
    data class AmountChanged(val value: String) : FuelRecordFormEvent
    data class LitersChanged(val value: String) : FuelRecordFormEvent
    data class CostCenterSelected(val id: String) : FuelRecordFormEvent
    data object UploadReceiptClicked : FuelRecordFormEvent
    data object ReceiptOptionsDismissed : FuelRecordFormEvent
    data class ReceiptPicked(val file: PickedFile?) : FuelRecordFormEvent
    data object RemoveReceipt : FuelRecordFormEvent
    data object Save : FuelRecordFormEvent
    data object Retry : FuelRecordFormEvent
    data object ResultDismissed : FuelRecordFormEvent
}
