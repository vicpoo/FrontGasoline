package org.vicpoo.gasolineproject.domain.reservations.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.ReservationDetail
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository

class GetReservationDetailUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(reservationId: String): AppResult<ReservationDetail> = repository.getDetail(reservationId)
}
