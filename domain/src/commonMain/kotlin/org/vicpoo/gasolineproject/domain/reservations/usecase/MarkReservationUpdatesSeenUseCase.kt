package org.vicpoo.gasolineproject.domain.reservations.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository

/** Al abrir la campana: las respuestas quedan como vistas. */
class MarkReservationUpdatesSeenUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.markUpdatesSeen()
}
