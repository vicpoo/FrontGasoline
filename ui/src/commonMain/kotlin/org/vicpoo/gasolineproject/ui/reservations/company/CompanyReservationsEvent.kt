package org.vicpoo.gasolineproject.ui.reservations.company

sealed interface CompanyReservationsEvent {
    data object Refresh : CompanyReservationsEvent

    /** Se llegó al final de la lista: pedir las 10 siguientes. */
    data object LoadMore : CompanyReservationsEvent
    data class FilterChanged(val filter: CompanyReservationsFilter) : CompanyReservationsEvent
}
