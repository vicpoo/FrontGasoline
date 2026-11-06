package org.vicpoo.gasolineproject.ui.triprecording.assigned

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicle
import org.vicpoo.gasolineproject.domain.triprecording.RecordedTrip
import org.vicpoo.gasolineproject.domain.triprecording.ReleaseBlock

data class AssignedVehiclesUiState(
    val vehicles: List<AssignedVehicle> = emptyList(),
    val activeTrip: RecordedTrip? = null,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: AppError? = null,
    /** Cuándo se trajo de la API la copia que se ve (para "Actualizado hace X"). */
    val lastUpdatedAtMillis: Long? = null,
    // ---- Dejar carro ----
    /** Carros que todavía no se pueden dejar y por qué (viaje en curso o sin enviar). */
    val releaseBlocks: Map<String, ReleaseBlock> = emptyMap(),
    val releaseTarget: AssignedVehicle? = null,
    val releaseNotes: String = "",
    val releaseNotesError: ValidationReason? = null,
    val isReleasing: Boolean = false,
    /** Se dejó con éxito (diálogo de resultado). */
    val released: AssignedVehicle? = null,
    val releaseError: AppError? = null,
) {
    /** null si se puede dejar; si no, el motivo que se muestra bajo el botón. */
    fun releaseBlockOf(vehicle: AssignedVehicle): ReleaseBlock? = releaseBlocks[vehicle.reservationId]
}
