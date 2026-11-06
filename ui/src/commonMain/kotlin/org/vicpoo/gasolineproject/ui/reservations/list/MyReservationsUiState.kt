package org.vicpoo.gasolineproject.ui.reservations.list

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.reservations.ReservationRequest

data class MyReservationsUiState(
    val requests: List<ReservationRequest> = emptyList(),
    /** Chip seleccionado y cuántas solicitudes hay en cada uno. */
    val filter: MyReservationsFilter = MyReservationsFilter.ALL,
    val counts: Map<MyReservationsFilter, Int> = emptyMap(),
    /** Solicitudes del filtro agrupadas por mes. */
    val groups: List<ReservationMonthGroup> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Sin conexión: se muestra la última copia guardada. */
    val isOffline: Boolean = false,
    val error: AppError? = null,
    /** Cuándo se trajo de la API la copia que se ve (para "Actualizado hace X"). */
    val lastUpdatedAtMillis: Long? = null,
)
