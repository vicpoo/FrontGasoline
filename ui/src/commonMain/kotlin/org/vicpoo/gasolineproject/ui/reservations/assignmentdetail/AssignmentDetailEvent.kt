package org.vicpoo.gasolineproject.ui.reservations.assignmentdetail

sealed interface AssignmentDetailEvent {
    data object Retry : AssignmentDetailEvent
    data class UnlinkClicked(val target: UnlinkTarget) : AssignmentDetailEvent
    data class UnlinkReasonChanged(val value: String) : AssignmentDetailEvent
    data object UnlinkConfirmed : AssignmentDetailEvent
    data object DialogDismissed : AssignmentDetailEvent
}
