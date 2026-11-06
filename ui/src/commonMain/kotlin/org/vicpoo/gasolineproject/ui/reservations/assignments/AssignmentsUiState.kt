package org.vicpoo.gasolineproject.ui.reservations.assignments

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.reservations.AssignmentsOverview

/** "Ver por": por carro o por empleado. */
enum class AssignmentsSegment { VEHICLES, EMPLOYEES }

data class AssignmentsUiState(
    /** Lo que llegó de la API (sin filtrar). */
    val overview: AssignmentsOverview = AssignmentsOverview(),
    /** Filtro "Ver por" elegido por el usuario. null = ninguno (la lista se ve por carros). */
    val segment: AssignmentsSegment? = null,
    val query: String = "",
    /** Vista de búsqueda y filtros abierta (como la de Vehículos). */
    val isSearchOpen: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: AppError? = null,
) {
    /** Cómo se agrupa la lista: lo que eligió el usuario o, si no eligió nada, por carros. */
    val viewBy: AssignmentsSegment get() = segment ?: AssignmentsSegment.VEHICLES

    /** Lo que se ve: filtrado por la búsqueda. */
    val visible: AssignmentsOverview get() = overview.filter(query)

    /** Insignia del buscador: cuántos filtros eligió el usuario. */
    val activeFilters: Int get() = if (segment != null) 1 else 0

    /** Hay algo escrito o un filtro elegido: se muestran resultados y "Borrar todo". */
    val hasSearchOrFilters: Boolean get() = query.isNotBlank() || activeFilters > 0
}
