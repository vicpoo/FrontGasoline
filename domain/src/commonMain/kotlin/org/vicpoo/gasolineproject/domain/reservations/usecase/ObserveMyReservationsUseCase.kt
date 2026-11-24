package org.vicpoo.gasolineproject.domain.reservations.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository
import org.vicpoo.gasolineproject.domain.reservations.ReservationRequest

/** Campana: las solicitudes del usuario y su estado (copia del teléfono). */
class ObserveMyReservationsUseCase(private val repository: ReservationRepository) {
    operator fun invoke(): Flow<List<ReservationRequest>> = repository.observeMine()
}
