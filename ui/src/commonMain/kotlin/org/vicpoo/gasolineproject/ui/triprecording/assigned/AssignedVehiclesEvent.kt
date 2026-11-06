package org.vicpoo.gasolineproject.ui.triprecording.assigned

import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicle

sealed interface AssignedVehiclesEvent {
    data object Refresh : AssignedVehiclesEvent
    data class ReleaseClicked(val vehicle: AssignedVehicle) : AssignedVehiclesEvent
    data class ReleaseNotesChanged(val value: String) : AssignedVehiclesEvent
    data object ReleaseConfirmed : AssignedVehiclesEvent
    data object DialogDismissed : AssignedVehiclesEvent
}
