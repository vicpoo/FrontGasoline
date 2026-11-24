package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.triprecording.RecordedTrip
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

/** "¿Está seguro de terminar con el viaje?" → Sí. */
class StopTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(): AppResult<RecordedTrip> = tracker.stop()
}
