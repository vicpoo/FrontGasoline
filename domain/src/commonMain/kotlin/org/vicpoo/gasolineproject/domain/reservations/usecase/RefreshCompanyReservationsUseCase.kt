package org.vicpoo.gasolineproject.domain.reservations.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository

class RefreshCompanyReservationsUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshCompany()
}
