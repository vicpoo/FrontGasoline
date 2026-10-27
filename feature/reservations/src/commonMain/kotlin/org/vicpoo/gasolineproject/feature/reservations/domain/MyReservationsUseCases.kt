package org.vicpoo.gasolineproject.feature.reservations.domain

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.core.common.AppResult

/** Campana: las solicitudes del usuario y su estado (copia del teléfono). */
class ObserveMyReservationsUseCase(private val repository: ReservationRepository) {
    operator fun invoke(): Flow<List<ReservationRequest>> = repository.observeMine()
}

/** Actualiza las solicitudes desde la API (sin red se queda la última copia). */
class RefreshMyReservationsUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshMine()
}

/** Número del badge de la campana (respuestas a mis solicitudes que no he visto). */
class ObserveUnseenReservationUpdatesUseCase(private val repository: ReservationRepository) {
    operator fun invoke(): Flow<Int> = repository.observeUnseenUpdates()
}

class RefreshUnseenReservationUpdatesUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshUnseenUpdates()
}

/** Al abrir la campana: las respuestas quedan como vistas. */
class MarkReservationUpdatesSeenUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.markUpdatesSeen()
}
