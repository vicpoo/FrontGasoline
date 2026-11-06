package org.vicpoo.gasolineproject.ui.triprecording.active

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecord
import org.vicpoo.gasolineproject.domain.triprecording.TrackingSession
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase

/** Privilegios de las recargas (cada acción tiene el suyo). */
data class FuelPermissions(
    val canAdd: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
)

data class ActiveTripUiState(
    val session: TrackingSession = TrackingSession(),
    val fuelRecords: List<FuelRecord> = emptyList(),
    /** Duración sin contar las pausas. */
    val elapsedMillis: Long = 0L,
    val fuel: FuelPermissions = FuelPermissions(),
    val showFinishConfirm: Boolean = false,
    val isChangingPause: Boolean = false,
    val isStopping: Boolean = false,
    val error: AppError? = null,
    /** true → ir a "Registrar Viaje" (kilometraje final, foto y ruta). */
    val navigateToFinish: Boolean = false,
    val deleteCandidate: FuelRecord? = null,
    val isDeleting: Boolean = false,
    /** true cuando ya se cargó el estado del tracker (para no salir antes de tiempo). */
    val isReady: Boolean = false,
) {
    val hasTrip: Boolean get() = session.trip != null
    val isPaused: Boolean get() = session.trip?.phase == TripPhase.PAUSED
}
