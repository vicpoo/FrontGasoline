package org.vicpoo.gasolineproject.domain.reservations.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository

/** Actualiza las solicitudes desde la API (sin red se queda la última copia). */
class RefreshMyReservationsUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshMine()
}
