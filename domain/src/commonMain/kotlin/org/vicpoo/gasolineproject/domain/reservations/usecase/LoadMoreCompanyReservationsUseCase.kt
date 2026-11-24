package org.vicpoo.gasolineproject.domain.reservations.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository

/** Pestaña llave: al bajar al final se piden las 10 siguientes. */
class LoadMoreCompanyReservationsUseCase(private val repository: ReservationRepository) {
    fun hasMore(): Flow<Boolean> = repository.observeCompanyHasMore()
    suspend operator fun invoke(): AppResult<Unit> = repository.loadMoreCompany()
}
