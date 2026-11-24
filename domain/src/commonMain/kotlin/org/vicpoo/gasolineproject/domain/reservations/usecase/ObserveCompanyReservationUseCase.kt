package org.vicpoo.gasolineproject.domain.reservations.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository
import org.vicpoo.gasolineproject.domain.reservations.ReservationRequest

/** Detalle de la solicitud: lo guardado en el teléfono se ve al instante; el detalle completo llega de la API. */
class ObserveCompanyReservationUseCase(private val repository: ReservationRepository) {
    operator fun invoke(reservationId: String): Flow<ReservationRequest?> = repository.observeCompanyRequest(reservationId)
}
