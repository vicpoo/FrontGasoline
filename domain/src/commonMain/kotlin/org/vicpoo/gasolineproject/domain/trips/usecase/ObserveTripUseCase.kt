package org.vicpoo.gasolineproject.domain.trips.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.trips.Trip
import org.vicpoo.gasolineproject.domain.trips.TripsRepository

class ObserveTripUseCase(private val repository: TripsRepository) {
    operator fun invoke(id: String): Flow<Trip?> = repository.observeTrip(id)
}
