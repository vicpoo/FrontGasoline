package org.vicpoo.gasolineproject.feature.reservations.domain

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason

/** Pestaña llave: todas las solicitudes de la empresa (copia del teléfono). */
class ObserveCompanyReservationsUseCase(private val repository: ReservationRepository) {
    operator fun invoke(): Flow<List<ReservationRequest>> = repository.observeCompany()
}

class RefreshCompanyReservationsUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshCompany()
}

/** Detalle de la solicitud: lo guardado en el teléfono se ve al instante; el detalle completo llega de la API. */
class ObserveCompanyReservationUseCase(private val repository: ReservationRepository) {
    operator fun invoke(reservationId: String): Flow<ReservationRequest?> = repository.observeCompanyRequest(reservationId)
}

class GetReservationDetailUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(reservationId: String): AppResult<ReservationDetail> = repository.getDetail(reservationId)
}

/**
 * Aceptar: la API ocupa el carro y rechaza las demás pendientes DE ESE MISMO CARRO
 * (las de otros carros no cambian).
 */
class ApproveReservationUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(reservationId: String): AppResult<Unit> = repository.approve(reservationId)
}

/** Rechazar: el motivo es obligatorio (lo ve el solicitante en la campana). */
class RejectReservationUseCase(private val repository: ReservationRepository) {
    suspend operator fun invoke(reservationId: String, reason: String): AppResult<Unit> {
        val clean = reason.trim()
        if (clean.isEmpty()) return AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED))
        if (clean.length > MAX_REASON) return AppResult.Failure(AppError.Validation(ValidationReason.OUT_OF_RANGE))
        return repository.reject(reservationId, clean)
    }

    companion object {
        const val MAX_REASON = 255
    }
}
