package org.vicpoo.gasolineproject.ui.triprecording.start

sealed interface StartTripEffect {
    data object RequestLocationPermission : StartTripEffect
    data object TripStarted : StartTripEffect
}
