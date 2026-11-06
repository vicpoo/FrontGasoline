package org.vicpoo.gasolineproject.ui.vehicles.list

import org.vicpoo.gasolineproject.domain.vehicles.model.Vehicle

/** Vista de búsqueda abierta (la que se abre al tocar la barra, estilo Instagram / Airbnb). */
data class VehicleSearchUiState(
    /** Lo que se está escribiendo / eligiendo; se aplica a la lista al cerrar la vista. */
    val draft: VehicleFilter = VehicleFilter(),
    /** Vehículos que coinciden con [draft] (para mostrarlos mientras se escribe y contarlos). */
    val results: List<Vehicle> = emptyList(),
)
