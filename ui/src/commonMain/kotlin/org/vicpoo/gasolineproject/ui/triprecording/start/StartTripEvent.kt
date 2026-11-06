package org.vicpoo.gasolineproject.ui.triprecording.start

sealed interface StartTripEvent {
    data class CostCenterSelected(val id: String) : StartTripEvent
    data object StartClicked : StartTripEvent
    data class PermissionResult(val granted: Boolean) : StartTripEvent
}
