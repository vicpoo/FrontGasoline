package org.vicpoo.gasolineproject.ui.triprecording.active

import org.vicpoo.gasolineproject.domain.triprecording.FuelRecord

sealed interface ActiveTripEvent {
    data object PauseClicked : ActiveTripEvent
    data object ResumeClicked : ActiveTripEvent
    data object FinishClicked : ActiveTripEvent
    data object FinishDismissed : ActiveTripEvent
    data object FinishConfirmed : ActiveTripEvent
    data object NavigationHandled : ActiveTripEvent
    data object ErrorDismissed : ActiveTripEvent
    data class DeleteClicked(val record: FuelRecord) : ActiveTripEvent
    data object DeleteDismissed : ActiveTripEvent
    data object DeleteConfirmed : ActiveTripEvent
    data object PermissionChanged : ActiveTripEvent
}
