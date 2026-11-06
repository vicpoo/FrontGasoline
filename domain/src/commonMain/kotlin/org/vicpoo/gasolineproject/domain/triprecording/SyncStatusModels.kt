package org.vicpoo.gasolineproject.domain.triprecording

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.sync.SyncAllPendingUseCase

/*
 * Diagnóstico de la sincronización: qué hay guardado en el teléfono sin enviar, por qué, y una
 * bitácora de los últimos intentos. Sirve para saber exactamente qué pasa cuando "no se envía".
 */

/** Resultado de un paso de la sincronización. */
enum class SyncLogResult {
    /** Llegó al servidor. */
    OK,

    /** Sin red, señal débil o tardó demasiado: se reintenta solo. */
    OFFLINE,

    /** El servidor lo rechazó (se guarda el motivo en el registro). */
    REJECTED,

    /** Otro problema (sesión terminada, error del servidor...). */
    ERROR,
}

/** Un paso de un intento de sincronización (ej. "Recarga 2#: enviar"). */
data class SyncLogEntry(
    /** Fecha y hora local del teléfono. */
    val at: String,
    val atMillis: Long,
    val step: String,
    val result: SyncLogResult,
    val message: String?,
    /** Código HTTP del rechazo, si lo hubo. */
    val code: Int?,
    val durationMs: Long,
)

/** Recarga que todavía no está en el servidor (o que el servidor rechazó). */
data class PendingFuelStatus(
    val localId: String,
    val number: Int,
    val liters: Double,
    val amount: Double?,
    val state: RecordSyncState,
    val errorMessage: String?,
)

/** En qué está un viaje que sigue guardado en el teléfono. */
enum class PendingTripState {
    /** En curso, en pausa o esperando el kilometraje final. */
    IN_PROGRESS,

    /** Terminado; falta que llegue todo al servidor (se envía solo). */
    WAITING_TO_SEND,

    /** El servidor lo rechazó y espera que el usuario lo corrija. */
    NEEDS_ATTENTION,
}

/** Viaje guardado en el teléfono y lo que le falta por enviar. */
data class PendingTripStatus(
    val localId: String,
    val vehicleTitle: String,
    val plates: String,
    val startedAt: String,
    val state: PendingTripState,
    /** El servidor ya conoce el viaje (tiene su id). */
    val registeredOnServer: Boolean,
    /** El servidor ya lo tiene como terminado. */
    val closedOnServer: Boolean,
    val errorMessage: String?,
    val pointsPending: Int,
    val pausesPending: Int,
    val odometerPhotoPending: Boolean,
    val fuel: List<PendingFuelStatus>,
)

/** Todo lo que el registro de viajes tiene sin enviar + la bitácora de intentos. */
data class SyncStatus(
    val trips: List<PendingTripStatus> = emptyList(),
    val log: List<SyncLogEntry> = emptyList(),
    val isSyncing: Boolean = false,
) {
    /** Recargas sin enviar o rechazadas (lo que más importa al operador). */
    val pendingFuelCount: Int get() = trips.sumOf { trip -> trip.fuel.count { it.state != RecordSyncState.SYNCED } }

    /** Viajes que siguen en el teléfono sin estar en curso (falta enviarlos o corregirlos). */
    val pendingTripCount: Int get() = trips.count { it.state != PendingTripState.IN_PROGRESS }

    val needsAttention: Boolean
        get() = trips.any { trip ->
            trip.state == PendingTripState.NEEDS_ATTENTION || trip.fuel.any { it.state == RecordSyncState.ERROR }
        }

    /** Último paso que llegó bien al servidor. */
    val lastSuccess: SyncLogEntry? get() = log.firstOrNull { it.result == SyncLogResult.OK }
}

/** Fuente del diagnóstico (la implementa la capa data). */
interface SyncStatusRepository {
    fun observe(): Flow<SyncStatus>
}

class ObserveSyncStatusUseCase(private val repository: SyncStatusRepository) {
    operator fun invoke(): Flow<SyncStatus> = repository.observe()
}

/**
 * "Enviar ahora": intenta mandar todo lo pendiente de TODA la app (viajes y recargas primero, luego
 * solicitudes y vehículos). Espera como máximo [waitMillis]; si con señal débil tarda más, el envío
 * SIGUE en el scope de la app aunque se salga de la pantalla (regresa null).
 */
class SyncNowUseCase(
    private val syncAll: SyncAllPendingUseCase,
    private val appScope: AppScope,
) {
    suspend operator fun invoke(waitMillis: Long = DEFAULT_WAIT_MS): AppResult<Unit>? {
        val running = appScope.scope.async { syncAll() }
        return withTimeoutOrNull(waitMillis) { running.await() }
    }

    private companion object {
        const val DEFAULT_WAIT_MS = 60_000L
    }
}
