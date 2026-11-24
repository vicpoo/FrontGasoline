package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

/** "Reintentar" en el diálogo de error: vuelve a intentar enviar el viaje guardado. */
class RetryFinishTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(): SendOutcome = tracker.retryFinishSend()
}
