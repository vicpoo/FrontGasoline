package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

/** El usuario respondió al permiso de ubicación: el tracker reanuda el GPS si hace falta. */
class LocationPermissionChangedUseCase(private val tracker: TripTracker) {
    operator fun invoke() = tracker.onPermissionChanged()
}
