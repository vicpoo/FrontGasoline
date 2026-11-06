package org.vicpoo.gasolineproject.ui.triprecording.syncstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.sync.ObservePendingSummaryUseCase
import org.vicpoo.gasolineproject.domain.sync.PendingSyncKind
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.triprecording.ObserveSyncStatusUseCase
import org.vicpoo.gasolineproject.domain.triprecording.SyncNowUseCase

/**
 * "Envíos pendientes": qué viajes y recargas siguen en el teléfono, por qué, y la bitácora de los
 * últimos intentos. También da el número para el aviso de la pestaña "Registrar".
 */
class SyncStatusViewModel(
    observeStatus: ObserveSyncStatusUseCase,
    observePendingSummary: ObservePendingSummaryUseCase,
    private val syncNow: SyncNowUseCase,
) : ViewModel() {

    /** Lo que solo vive en la pantalla (no viene de los datos). */
    private data class LocalUi(
        val sending: Boolean = false,
        val result: SendNowResult? = null,
        val showFullLog: Boolean = false,
    )

    private val local = MutableStateFlow(LocalUi())

    /** Los viajes y recargas ya vienen con detalle en observeStatus: aquí solo el resto de la app. */
    private val others = observePendingSummary().map { items -> items.filter { it.kind != PendingSyncKind.TRIPS } }

    val state: StateFlow<SyncStatusUiState> = combine(observeStatus(), others, local) { status, others, ui ->
        SyncStatusUiState(
            others = others,
            isLoading = false,
            status = status,
            isSending = ui.sending || status.isSyncing,
            sendResult = ui.result,
            showFullLog = ui.showFullLog,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SyncStatusUiState())

    fun onEvent(event: SyncStatusEvent) {
        when (event) {
            SyncStatusEvent.SendNowClicked -> sendNow()
            SyncStatusEvent.ResultDismissed -> local.update { it.copy(result = null) }
            SyncStatusEvent.ToggleFullLog -> local.update { it.copy(showFullLog = !it.showFullLog) }
        }
    }

    private fun sendNow() {
        if (local.value.sending) return
        local.update { it.copy(sending = true, result = null) }
        viewModelScope.launch {
            // El envío corre en el scope de la app: si se sale de la pantalla, sigue
            val result = when (val sync = syncNow()) {
                null -> SendNowResult.STILL_SENDING
                is AppResult.Success -> SendNowResult.DONE
                is AppResult.Failure -> if (sync.error == AppError.NoConnection) SendNowResult.OFFLINE else SendNowResult.ERROR
            }
            local.update { it.copy(sending = false, result = result) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
