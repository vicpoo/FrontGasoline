package org.vicpoo.gasolineproject.core.sync

import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult

/*
 * Sincronización offline-first compartida por todas las features.
 *
 * - Cada feature que guarda cambios en el teléfono implementa [PendingSyncTask] y la registra en
 *   Koin con `bind PendingSyncTask::class` (Open/Closed: una feature nueva no toca este archivo).
 * - [SyncAllPendingUseCase] las ejecuta en orden; lo usa el trabajo en segundo plano.
 * - [BackgroundSyncScheduler] es la abstracción del planificador de cada plataforma
 *   (WorkManager en Android, nada en iOS por ahora). Dependency Inversion: las features solo
 *   conocen esta interfaz.
 */

/** Algo que tiene cambios guardados en el teléfono y sabe enviarlos a la API. */
interface PendingSyncTask {
    /**
     * Envía lo pendiente. Regresa Failure SOLO cuando hay que detener todo
     * (sin red o sesión vencida); los rechazos de la API se guardan en cada registro.
     */
    suspend fun syncPending(): AppResult<Unit>
}

/** Planificador de la sincronización en segundo plano (implementación por plataforma). */
interface BackgroundSyncScheduler {
    /** Sincronización periódica mientras haya sesión (solo con red). */
    fun schedulePeriodic()

    /** Una sincronización en cuanto haya red (ej. algo se guardó sin conexión). */
    fun requestWhenOnline()

    /** Cancela todo (al cerrar sesión). */
    fun cancel()
}

/** true si el error significa "sin red / sesión vencida": se detiene y se reintenta después. */
fun AppError.stopsSync(): Boolean = this == AppError.NoConnection || this == AppError.Unauthorized

/**
 * Reglas de reintento de un registro que la API rechazó (Single Responsibility).
 * - 4xx (400, 403, 404, 409, 422...): el dato está mal; reenviarlo daría el mismo error.
 *   Se deja de intentar en automático hasta que el usuario lo edite o toque "Reintentar".
 * - 408, 429, 5xx o error desconocido: problema temporal, se reintenta en la siguiente vuelta.
 */
object SyncRejection {
    /** Código que se guarda cuando el rechazo lo detecta la propia app (ej. falta la foto del recibo). */
    const val LOCAL_INVALID = 422

    fun isPermanent(code: Int?): Boolean =
        code != null && code in 400..499 && code != 408 && code != 429
}

/** Ejecuta todas las [PendingSyncTask] registradas. Se detiene en el primer "sin red / sesión vencida". */
class SyncAllPendingUseCase(private val tasks: List<PendingSyncTask>) {
    suspend operator fun invoke(): AppResult<Unit> {
        for (task in tasks) {
            val result = task.syncPending()
            if (result is AppResult.Failure && result.error.stopsSync()) return result
        }
        return AppResult.Success(Unit)
    }
}
