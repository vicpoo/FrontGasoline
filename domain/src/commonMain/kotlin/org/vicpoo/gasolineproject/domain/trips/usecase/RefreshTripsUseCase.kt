package org.vicpoo.gasolineproject.domain.trips.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.trips.TripsRepository

class RefreshTripsUseCase(private val repository: TripsRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}
