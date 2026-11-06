package org.vicpoo.gasolineproject.ui.triprecording.syncstatus

import org.vicpoo.gasolineproject.domain.sync.PendingSummaryItem
import org.vicpoo.gasolineproject.domain.triprecording.SyncStatus

/** Resultado del botón "Enviar ahora". */
enum class SendNowResult {
    /** Terminó: llegó todo lo que el servidor aceptó (lo rechazado queda marcado). */
    DONE,

    /** Con señal débil tardó más de un minuto: se sigue enviando en segundo plano. */
    STILL_SENDING,

    /** Sin conexión: se reintenta solo cuando vuelva la red. */
    OFFLINE,

    /** La sesión terminó u otro error: hay que revisar. */
    ERROR,
}

/** Pantalla "Envíos pendientes" (diagnóstico de la sincronización de viajes y recargas). */
data class SyncStatusUiState(
    val isLoading: Boolean = true,
    val status: SyncStatus = SyncStatus(),
    /** Hay un envío en curso (lo pidió el usuario o la sincronización automática). */
    val isSending: Boolean = false,
    val sendResult: SendNowResult? = null,
    /** Mostrar toda la bitácora (por defecto solo los últimos pasos). */
    val showFullLog: Boolean = false,
    /** Pendientes del resto de la app (solicitudes de carro, vehículos...), sin los viajes. */
    val others: List<PendingSummaryItem> = emptyList(),
) {
    val isAllSent: Boolean get() = status.trips.isEmpty() && others.isEmpty()

    /** Registros sin enviar de toda la app (el número del aviso en "Registrar"). */
    val pendingCount: Int get() = status.pendingFuelCount + status.pendingTripCount + others.sumOf { it.count }
}
