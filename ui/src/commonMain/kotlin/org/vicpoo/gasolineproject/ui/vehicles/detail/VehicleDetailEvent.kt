package org.vicpoo.gasolineproject.ui.vehicles.detail

sealed interface VehicleDetailEvent {
    data object DeleteClicked : VehicleDetailEvent
    data object DeleteDismissed : VehicleDetailEvent
    data object DeleteConfirmed : VehicleDetailEvent

    data object RequestClicked : VehicleDetailEvent
    data class RequestReasonChanged(val value: String) : VehicleDetailEvent
    data object RequestDismissed : VehicleDetailEvent
    data object SendRequest : VehicleDetailEvent
    data object RequestResultDismissed : VehicleDetailEvent
}
