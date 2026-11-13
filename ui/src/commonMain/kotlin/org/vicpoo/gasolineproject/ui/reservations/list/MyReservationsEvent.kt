package org.vicpoo.gasolineproject.ui.reservations.list

sealed interface MyReservationsEvent {
    data object Refresh : MyReservationsEvent

    /** Se llegó al final de la lista: pedir las 10 siguientes. */
    data object LoadMore : MyReservationsEvent
    data class FilterSelected(val filter: MyReservationsFilter) : MyReservationsEvent
}
