package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.triprecording.RecordedTrip
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

class ResumeTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(): AppResult<RecordedTrip> = tracker.resume()
}
