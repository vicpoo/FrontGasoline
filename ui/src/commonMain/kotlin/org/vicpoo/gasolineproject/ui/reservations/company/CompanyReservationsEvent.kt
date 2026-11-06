package org.vicpoo.gasolineproject.ui.reservations.company

sealed interface CompanyReservationsEvent {
    data object Refresh : CompanyReservationsEvent
    data class FilterChanged(val filter: CompanyReservationsFilter) : CompanyReservationsEvent
}
