package org.vicpoo.gasolineproject.ui.triprecording.finish

import org.vicpoo.gasolineproject.domain.files.PickedFile

sealed interface FinishTripEvent {
    data class FinalMileageChanged(val value: String) : FinishTripEvent
    data object PhotoClicked : FinishTripEvent
    data object PhotoOptionsDismissed : FinishTripEvent
    data class PhotoPicked(val file: PickedFile?) : FinishTripEvent
    data object RemovePhoto : FinishTripEvent
    data object Save : FinishTripEvent
    data object Retry : FinishTripEvent
    /** Cierra el diálogo de resultado. */
    data object ResultDismissed : FinishTripEvent
}
