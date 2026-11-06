package org.vicpoo.gasolineproject.ui.reservations.company

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.reservations.ReservationRequest

/** Filtro de la lista: primero lo que falta por decidir. */
enum class CompanyReservationsFilter { PENDING, ALL }

data class CompanyReservationsUiState(
    val requests: List<ReservationRequest> = emptyList(),
    val filter: CompanyReservationsFilter = CompanyReservationsFilter.PENDING,
    val pendingCount: Int = 0,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: AppError? = null,
    /** Cuándo se trajo de la API la copia que se ve (para "Actualizado hace X"). */
    val lastUpdatedAtMillis: Long? = null,
)
