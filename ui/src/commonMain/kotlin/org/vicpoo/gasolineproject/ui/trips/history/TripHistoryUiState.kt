package org.vicpoo.gasolineproject.ui.trips.history

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.trips.Trip

data class TripHistoryUiState(
    /** Solo los viajes que ya se muestran (se agregan de 4 en 4 al bajar). */
    val trips: List<Trip> = emptyList(),
    /** Faltan viajes por mostrar (se muestra el indicador al final de la lista). */
    val hasMore: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: AppError? = null,
    /** Cuándo se trajo de la API la copia que se ve (para "Actualizado hace X"). */
    val lastUpdatedAtMillis: Long? = null,
)
