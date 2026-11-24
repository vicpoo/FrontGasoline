package org.vicpoo.gasolineproject.domain.trips.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.trips.TripsRepository

class LoadTripDetailsUseCase(private val repository: TripsRepository) {
    suspend operator fun invoke(tripIds: List<String>): AppResult<Unit> = repository.loadDetails(tripIds)
}
