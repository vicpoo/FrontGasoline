package org.vicpoo.gasolineproject.domain.trips.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.trips.Trip
import org.vicpoo.gasolineproject.domain.trips.TripsRepository

class ObserveTripsUseCase(private val repository: TripsRepository) {
    operator fun invoke(): Flow<List<Trip>> = repository.observeTrips()
}
