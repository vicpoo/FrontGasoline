package org.vicpoo.gasolineproject.ui.reservations.detail

sealed interface ReservationDetailEvent {
    data object Retry : ReservationDetailEvent
    data object ApproveClicked : ReservationDetailEvent
    data object ApproveConfirmed : ReservationDetailEvent
    data object RejectClicked : ReservationDetailEvent
    data class RejectReasonChanged(val value: String) : ReservationDetailEvent
    data object RejectConfirmed : ReservationDetailEvent
    data object DialogDismissed : ReservationDetailEvent
    data object LicenseClicked : ReservationDetailEvent
}
