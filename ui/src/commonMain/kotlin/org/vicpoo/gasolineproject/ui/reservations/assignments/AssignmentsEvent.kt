package org.vicpoo.gasolineproject.ui.reservations.assignments

sealed interface AssignmentsEvent {
    data object Refresh : AssignmentsEvent

    /** Tocar un filtro lo elige; tocarlo otra vez lo quita (como en Vehículos). */
    data class SegmentToggled(val segment: AssignmentsSegment) : AssignmentsEvent
    data class QueryChanged(val value: String) : AssignmentsEvent
    data object SearchOpened : AssignmentsEvent
    data object SearchDismissed : AssignmentsEvent

    /** X del buscador / "Borrar todo": sin texto y sin filtros. */
    data object SearchCleared : AssignmentsEvent
}
