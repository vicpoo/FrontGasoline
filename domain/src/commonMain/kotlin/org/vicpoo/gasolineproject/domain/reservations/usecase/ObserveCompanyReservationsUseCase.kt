package org.vicpoo.gasolineproject.domain.reservations.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository
import org.vicpoo.gasolineproject.domain.reservations.ReservationRequest

/** Pestaña llave: todas las solicitudes de la empresa (copia del teléfono). */
class ObserveCompanyReservationsUseCase(private val repository: ReservationRepository) {
    operator fun invoke(): Flow<List<ReservationRequest>> = repository.observeCompany()
}
