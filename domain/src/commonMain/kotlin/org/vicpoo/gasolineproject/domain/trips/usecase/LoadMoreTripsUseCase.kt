package org.vicpoo.gasolineproject.domain.trips.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.trips.TripsRepository

/** Historial: al bajar al final se piden los 10 viajes siguientes. */
class LoadMoreTripsUseCase(private val repository: TripsRepository) {
    fun hasMore(): Flow<Boolean> = repository.observeHasMore()
    suspend operator fun invoke(): AppResult<Unit> = repository.loadMore()
}
