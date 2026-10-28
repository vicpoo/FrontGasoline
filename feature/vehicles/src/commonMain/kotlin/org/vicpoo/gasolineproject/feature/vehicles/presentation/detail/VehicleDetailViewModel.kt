package org.vicpoo.gasolineproject.feature.vehicles.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.rbac.PermissionChecker
import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.feature.reservations.api.RequestOutcome
import org.vicpoo.gasolineproject.feature.reservations.api.RequestedVehicle
import org.vicpoo.gasolineproject.feature.reservations.api.VehicleRequester
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehiclePhoto
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleStatus
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.DeleteVehicleUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObserveVehicleUseCase
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehiclesMode

/** Argumentos de la pantalla (se pasan con Koin parametersOf). */
data class VehicleDetailArgs(
    val vehicleId: String,
    /** CATALOG: solo ver y solicitar. FLEET: editar / eliminar. */
    val mode: VehiclesMode,
)

data class VehicleDetailUiState(
    val vehicle: Vehicle? = null,
    val isLoading: Boolean = true,
    /** RBAC */
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    /** RBAC (solicitud.crear) + el carro debe estar disponible. */
    val canRequest: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isDeleting: Boolean = false,
    val error: AppError? = null,
    // ---- Solicitud de reserva ----
    val showRequestDialog: Boolean = false,
    val requestReason: String = "",
    val requestReasonError: ValidationReason? = null,
    val isSendingRequest: Boolean = false,
    val requestError: AppError? = null,
    val requestSent: Boolean = false,
    /** Sin conexión: la solicitud quedó guardada y se enviará sola. */
    val requestQueued: Boolean = false,
)

sealed interface VehicleDetailEvent {
    data object DeleteClicked : VehicleDetailEvent
    data object DeleteDismissed : VehicleDetailEvent
    data object DeleteConfirmed : VehicleDetailEvent

    data object RequestClicked : VehicleDetailEvent
    data class RequestReasonChanged(val value: String) : VehicleDetailEvent
    data object RequestDismissed : VehicleDetailEvent
    data object SendRequest : VehicleDetailEvent
    data object RequestResultDismissed : VehicleDetailEvent
}

sealed interface VehicleDetailEffect {
    data object Deleted : VehicleDetailEffect
}

class VehicleDetailViewModel(
    private val args: VehicleDetailArgs,
    observeVehicle: ObserveVehicleUseCase,
    private val deleteVehicle: DeleteVehicleUseCase,
    private val requestVehicle: VehicleRequester,
    permissionChecker: PermissionChecker,
) : ViewModel() {

    private data class Permissions(val edit: Boolean, val delete: Boolean, val request: Boolean)

    private val local = MutableStateFlow(VehicleDetailUiState())

    private val permissions = combine(
        permissionChecker.observe(Privileges.CARRO_EDITAR),
        permissionChecker.observe(Privileges.CARRO_ELIMINAR),
        permissionChecker.observe(Privileges.SOLICITUD_CREAR),
    ) { edit, delete, request -> Permissions(edit, delete, request) }

    val state: StateFlow<VehicleDetailUiState> = combine(
        observeVehicle(args.vehicleId),
        permissions,
        local,
    ) { vehicle, perms, ui ->
        ui.copy(
            vehicle = vehicle,
            isLoading = false,
            canEdit = perms.edit && args.mode == VehiclesMode.FLEET,
            canDelete = perms.delete && args.mode == VehiclesMode.FLEET,
            canRequest = perms.request && args.mode == VehiclesMode.CATALOG && vehicle?.status == VehicleStatus.AVAILABLE,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleDetailUiState())

    private val _effects = Channel<VehicleDetailEffect>(Channel.BUFFERED)
    val effects: Flow<VehicleDetailEffect> = _effects.receiveAsFlow()

    fun onEvent(event: VehicleDetailEvent) {
        when (event) {
            VehicleDetailEvent.DeleteClicked -> local.update { it.copy(showDeleteConfirm = true, error = null) }
            VehicleDetailEvent.DeleteDismissed -> local.update { it.copy(showDeleteConfirm = false) }
            VehicleDetailEvent.DeleteConfirmed -> delete()

            VehicleDetailEvent.RequestClicked -> local.update {
                it.copy(showRequestDialog = true, requestReason = "", requestReasonError = null)
            }
            is VehicleDetailEvent.RequestReasonChanged -> local.update {
                it.copy(requestReason = event.value.take(VehicleRequester.MAX_REASON), requestReasonError = null)
            }
            VehicleDetailEvent.RequestDismissed -> local.update { it.copy(showRequestDialog = false) }
            VehicleDetailEvent.SendRequest -> sendRequest()
            VehicleDetailEvent.RequestResultDismissed -> local.update { it.copy(requestError = null, requestSent = false, requestQueued = false) }
        }
    }

    private fun sendRequest() {
        if (local.value.isSendingRequest) return
        viewModelScope.launch {
            local.update { it.copy(isSendingRequest = true, requestError = null) }
            val vehicle = state.value.vehicle
            val requested = RequestedVehicle(
                serverId = vehicle?.serverId,
                title = vehicle?.brandAndModel,
                plates = vehicle?.plates,
                photoUrl = (vehicle?.photo as? VehiclePhoto.Remote)?.url,
            )
            val result = requestVehicle(requested, local.value.requestReason)
            local.update {
                when (result) {
                    is AppResult.Success -> it.copy(
                        isSendingRequest = false,
                        showRequestDialog = false,
                        requestSent = result.data == RequestOutcome.SENT,
                        requestQueued = result.data == RequestOutcome.QUEUED,
                    )
                    is AppResult.Failure -> {
                        val error = result.error
                        if (error is AppError.Validation) {
                            it.copy(isSendingRequest = false, requestReasonError = error.reason)
                        } else {
                            it.copy(isSendingRequest = false, showRequestDialog = false, requestError = error)
                        }
                    }
                }
            }
        }
    }

    private fun delete() {
        if (local.value.isDeleting) return
        viewModelScope.launch {
            local.update { it.copy(isDeleting = true) }
            when (val result = deleteVehicle(args.vehicleId)) {
                is AppResult.Success -> {
                    local.update { it.copy(isDeleting = false, showDeleteConfirm = false) }
                    _effects.send(VehicleDetailEffect.Deleted)
                }
                is AppResult.Failure -> local.update {
                    it.copy(isDeleting = false, showDeleteConfirm = false, error = result.error)
                }
            }
        }
    }
}
