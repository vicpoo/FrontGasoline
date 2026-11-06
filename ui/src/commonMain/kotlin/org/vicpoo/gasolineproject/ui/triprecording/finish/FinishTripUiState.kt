package org.vicpoo.gasolineproject.ui.triprecording.finish

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripField
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome

data class FinishTripUiState(
    val isLoading: Boolean = true,
    /** No hay viaje detenido esperando el kilometraje (ej. ya se guardó). */
    val notFound: Boolean = false,
    val initialMileageText: String = "",
    val gpsDistanceText: String = "0",
    val durationText: String = "",
    val route: List<GeoPoint> = emptyList(),
    val finalMileage: String = "",
    val photo: PickedFile? = null,
    val errors: Map<FinishTripField, ValidationReason> = emptyMap(),
    val showPhotoOptions: Boolean = false,
    val isSaving: Boolean = false,
    val outcome: SendOutcome? = null,
    val failure: AppError? = null,
    /** true → regresar a la lista de carros (el viaje ya quedó guardado). */
    val done: Boolean = false,
)
