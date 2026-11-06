package org.vicpoo.gasolineproject.ui.reservations.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.rbac.PermissionChecker
import org.vicpoo.gasolineproject.domain.rbac.Privileges
import org.vicpoo.gasolineproject.domain.reservations.ApproveReservationUseCase
import org.vicpoo.gasolineproject.domain.reservations.GetReservationDetailUseCase
import org.vicpoo.gasolineproject.domain.reservations.ObserveCompanyReservationUseCase
import org.vicpoo.gasolineproject.domain.reservations.RejectReservationUseCase

/**
 * Detalle de una solicitud (pestaña llave): motivo, quién la pide con su foto y su licencia,
 * qué carro y su estado. Aquí se acepta o se rechaza. Aceptar rechaza solas las demás
 * pendientes DEL MISMO CARRO (lo hace la API).
 */
class ReservationDetailViewModel(
    private val reservationId: String,
    observeRequest: ObserveCompanyReservationUseCase,
    permissionChecker: PermissionChecker,
    private val getDetail: GetReservationDetailUseCase,
    private val approveReservation: ApproveReservationUseCase,
    private val rejectReservation: RejectReservationUseCase,
) : ViewModel() {

    private val ui = MutableStateFlow(ReservationDetailUiState())
    private val _state = MutableStateFlow(ReservationDetailUiState())
    val state: StateFlow<ReservationDetailUiState> = _state.asStateFlow()

    init {
        val permissions = combine(
            permissionChecker.observe(Privileges.SOLICITUD_APROBAR),
            permissionChecker.observe(Privileges.SOLICITUD_RECHAZAR),
        ) { approve, reject -> approve to reject }

        viewModelScope.launch {
            combine(observeRequest(reservationId), ui, permissions) { cached, current, (approve, reject) ->
                current.copy(request = cached, canApprove = approve, canReject = reject)
            }.collect { _state.value = it }
        }
        load()
    }

    fun onEvent(event: ReservationDetailEvent) {
        when (event) {
            ReservationDetailEvent.Retry -> load()
            ReservationDetailEvent.ApproveClicked -> ui.update { it.copy(showApproveConfirm = true) }
            ReservationDetailEvent.ApproveConfirmed -> approve()
            ReservationDetailEvent.RejectClicked -> ui.update {
                it.copy(showRejectDialog = true, rejectReason = "", rejectReasonError = null)
            }
            is ReservationDetailEvent.RejectReasonChanged -> ui.update {
                it.copy(rejectReason = event.value, rejectReasonError = null)
            }
            ReservationDetailEvent.RejectConfirmed -> reject()
            ReservationDetailEvent.LicenseClicked -> ui.update { it.copy(showLicense = true) }
            ReservationDetailEvent.DialogDismissed -> ui.update {
                if (it.isSending) it else it.copy(
                    showApproveConfirm = false,
                    showRejectDialog = false,
                    decision = null,
                    decisionError = null,
                    showLicense = false,
                )
            }
        }
    }

    private fun load() {
        ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getDetail(reservationId)) {
                is AppResult.Success -> ui.update { it.copy(detail = result.data, isLoading = false, isOffline = false) }
                is AppResult.Failure -> ui.update {
                    it.copy(
                        isLoading = false,
                        isOffline = result.error == AppError.NoConnection,
                        error = result.error.takeIf { error -> error != AppError.NoConnection },
                    )
                }
            }
        }
    }

    private fun approve() {
        if (ui.value.isSending) return
        ui.update { it.copy(isSending = true) }
        viewModelScope.launch {
            val result = approveReservation(reservationId)
            ui.update {
                it.copy(
                    isSending = false,
                    showApproveConfirm = false,
                    decision = if (result is AppResult.Success) ReservationDecision.APPROVED else null,
                    decisionError = (result as? AppResult.Failure)?.error,
                )
            }
            // Con éxito la pantalla regresa sola a la lista (la lista ya se actualizó en el repositorio)
        }
    }

    private fun reject() {
        if (ui.value.isSending) return
        ui.update { it.copy(isSending = true) }
        viewModelScope.launch {
            when (val result = rejectReservation(reservationId, ui.value.rejectReason)) {
                is AppResult.Success -> {
                    ui.update { it.copy(isSending = false, showRejectDialog = false, decision = ReservationDecision.REJECTED) }
                }
                is AppResult.Failure -> ui.update {
                    val validation = (result.error as? AppError.Validation)?.reason
                    if (validation != null) {
                        it.copy(isSending = false, rejectReasonError = validation)
                    } else {
                        it.copy(isSending = false, showRejectDialog = false, decisionError = result.error)
                    }
                }
            }
        }
    }
}
