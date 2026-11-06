package org.vicpoo.gasolineproject.ui.trips.detail

import org.vicpoo.gasolineproject.domain.trips.Trip

data class TripDetailUiState(
    val trip: Trip? = null,
    val isLoading: Boolean = true,
    /** URL del recibo abierto en el visor (null = cerrado). */
    val openReceiptUrl: String? = null,
)
