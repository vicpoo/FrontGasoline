package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordRepository
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import org.vicpoo.gasolineproject.domain.triprecording.outcomeFor

/** Reintenta enviar una recarga que quedó pendiente o rechazada (botón "Reintentar"). */
class RetryFuelRecordUseCase(
    private val repository: FuelRecordRepository,
    private val sync: TripRecordingSync,
) {
    suspend operator fun invoke(localId: String): SendOutcome {
        repository.markForRetry(localId)
        return sync.outcomeFor(localId) { repository.get(it) }
    }
}
