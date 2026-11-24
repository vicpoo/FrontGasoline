package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.triprecording.RecordedTrip
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

/** Regresar desde "Registrar Viaje" sin guardar: el viaje continúa. */
class UndoStopTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(): AppResult<RecordedTrip> = tracker.undoStop()
}
