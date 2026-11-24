package org.vicpoo.gasolineproject.domain.reservations.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository

/**
 * Aceptar: la API ocupa el carro y rechaza las demás pendientes DE ESE MISMO CARRO
 * (las de otros carros no cambian).
 */
class ApproveReservationUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(reservationId: String): AppResult<Unit> = repository.approve(reservationId)
}
