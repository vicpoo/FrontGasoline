package org.vicpoo.gasolineproject.domain.reservations.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository

/** "Mis solicitudes": al bajar al final se piden las 10 siguientes. */
class LoadMoreMyReservationsUseCase(private val repository: ReservationRepository) {
    fun hasMore(): Flow<Boolean> = repository.observeMineHasMore()
    suspend operator fun invoke(): AppResult<Unit> = repository.loadMoreMine()
}
