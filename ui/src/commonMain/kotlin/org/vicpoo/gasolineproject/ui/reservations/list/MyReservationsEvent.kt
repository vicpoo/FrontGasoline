package org.vicpoo.gasolineproject.ui.reservations.list

sealed interface MyReservationsEvent {
    data object Refresh : MyReservationsEvent
    data class FilterSelected(val filter: MyReservationsFilter) : MyReservationsEvent
}
