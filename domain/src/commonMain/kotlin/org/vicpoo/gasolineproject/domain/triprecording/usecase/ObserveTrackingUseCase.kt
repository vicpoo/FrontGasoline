package org.vicpoo.gasolineproject.domain.triprecording.usecase

import kotlinx.coroutines.flow.StateFlow
import org.vicpoo.gasolineproject.domain.triprecording.TrackingSession
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

class ObserveTrackingUseCase(private val tracker: TripTracker) {
    operator fun invoke(): StateFlow<TrackingSession> = tracker.session
}
