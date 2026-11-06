package org.vicpoo.gasolineproject.ui.vehicles.list

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.vehicles.model.Vehicle
import org.vicpoo.gasolineproject.domain.vehicles.model.VehicleStatus
import org.vicpoo.gasolineproject.domain.vehicles.model.VehicleType

/**
 * Búsqueda + filtros de la lista. Vacío = se ven todos.
 * Los filtros dentro de un grupo se combinan con "o" (Disponible o Mantenimiento) y entre grupos
 * con "y" (Moto y Disponible).
 */
data class VehicleFilter(
    val query: String = "",
    val statuses: Set<VehicleStatus> = emptySet(),
    val types: Set<VehicleType> = emptySet(),
) {
    /** Cuántos filtros hay (la búsqueda por texto no cuenta; va escrita en la barra). */
    val activeFilters: Int get() = statuses.size + types.size

    val isEmpty: Boolean get() = query.isBlank() && activeFilters == 0

    fun matches(vehicle: Vehicle): Boolean {
        if (statuses.isNotEmpty() && vehicle.status !in statuses) return false
        if (types.isNotEmpty() && vehicle.type !in types) return false
        val text = query.trim()
        if (text.isEmpty()) return true
        // Cada palabra debe aparecer en algún dato ("nissan blanco", "abc 123")
        val haystack = listOfNotNull(vehicle.brand, vehicle.model, vehicle.plates, vehicle.color).joinToString(" ")
        return text.split(' ').filter { it.isNotBlank() }.all { word ->
            haystack.contains(word, ignoreCase = true) || vehicle.plates.replace("-", "").contains(word.replace("-", ""), ignoreCase = true)
        }
    }
}

data class VehicleListUiState(
    val vehicles: List<Vehicle> = emptyList(),
    /** Búsqueda y filtros aplicados a la lista. */
    val filter: VehicleFilter = VehicleFilter(),
    /** null = la vista de búsqueda está cerrada. */
    val search: VehicleSearchUiState? = null,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val pendingSyncCount: Int = 0,
    /** RBAC: mostrar botón "+" */
    val canAdd: Boolean = false,
    val error: AppError? = null,
    /** Cuándo se trajo de la API la copia que se ve (para "Actualizado hace X"). */
    val lastUpdatedAtMillis: Long? = null,
)

/**
 * Para qué se abre la lista:
 * - CATALOG: pestaña Inicio (casa) — ver carros y solicitarlos; sin agregar / editar / eliminar.
 * - FLEET: pestaña Gestión (libreta con lápiz) — administrar la flota; sin "Solicitar".
 */
enum class VehiclesMode { CATALOG, FLEET }
