package org.vicpoo.gasolineproject.domain.reservations.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository

/** Número del badge de la campana (respuestas a mis solicitudes que no he visto). */
class ObserveUnseenReservationUpdatesUseCase(private val repository: ReservationRepository) {
    operator fun invoke(): Flow<Int> = repository.observeUnseenUpdates()
}
