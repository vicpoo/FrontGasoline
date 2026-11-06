package org.vicpoo.gasolineproject.ui.vehicles.list

import org.vicpoo.gasolineproject.domain.vehicles.model.VehicleStatus
import org.vicpoo.gasolineproject.domain.vehicles.model.VehicleType

sealed interface VehicleListEvent {
    data object Refresh : VehicleListEvent

    // ---- Vista de búsqueda ----
    /** Tocar la barra: abre la vista con lo que ya estaba aplicado. */
    data object SearchOpened : VehicleListEvent
    /** Regresar (‹ o atrás): sin botón de "Ver resultados", lo elegido queda aplicado al salir. */
    data object SearchDismissed : VehicleListEvent
    data class QueryChanged(val value: String) : VehicleListEvent
    data class StatusToggled(val status: VehicleStatus) : VehicleListEvent
    data class TypeToggled(val type: VehicleType) : VehicleListEvent
    /** "Borrar todo" dentro de la vista. */
    data object DraftCleared : VehicleListEvent
    /** La tecla buscar del teclado: aplica y cierra. */
    data object SearchApplied : VehicleListEvent

    /** La X de la barra: quita búsqueda y filtros sin abrir la vista. */
    data object FilterCleared : VehicleListEvent
}
