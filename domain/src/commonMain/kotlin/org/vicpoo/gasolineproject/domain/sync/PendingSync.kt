package org.vicpoo.gasolineproject.domain.sync

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult

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

/**
 * Qué se envía. Además de nombrar el grupo en "Envíos pendientes", define el ORDEN de envío:
 * primero lo más importante (viajes y recargas), al final lo que se puede rehacer.
 */
enum class PendingSyncKind {
    /** Viajes, pausas, ruta GPS y recargas de gasolina. */
    TRIPS,

    /** Solicitudes de carro hechas sin red. */
    RESERVATIONS,

    /** Altas, cambios y bajas de vehículos (gestión de la flota). */
    VEHICLES,

    /** Registro del celular para recibir notificaciones. */
    DEVICE,
    OTHER,
}

/** Algo que tiene cambios guardados en el teléfono y sabe enviarlos a la API. */
interface PendingSyncTask {
    /** Grupo y prioridad de envío (ver [PendingSyncKind]). */
    val kind: PendingSyncKind get() = PendingSyncKind.OTHER

    /**
     * Envía lo pendiente. Regresa Failure SOLO cuando hay que detener todo
     * (sin red o sesión vencida); los rechazos de la API se guardan en cada registro.
     */
    suspend fun syncPending(): AppResult<Unit>

    /**
     * Cuántos registros siguen guardados SOLO en el teléfono (sin llegar a la API, o rechazados).
     * Cerrar sesión los borra, así que antes se avisa al usuario (ver [PendingBeforeLogoutUseCase]).
     */
    suspend fun pendingCount(): Int = 0
}

/**
 * Antes de "Salir": si hay registros que solo existen en el teléfono, intenta mandarlos una vez y regresa
 * cuántos siguen pendientes (0 = se puede cerrar sesión sin perder nada).
 */
class PendingBeforeLogoutUseCase(private val tasks: List<PendingSyncTask>) {
    suspend operator fun invoke(): Int {
        if (countPending() == 0) return 0
        SyncAllPendingUseCase(tasks)()
        return countPending()
    }

    private suspend fun countPending(): Int = tasks.sumOf { it.pendingCount() }
}

/** Cuántos registros de TODA la app siguen solo en el teléfono. */
class CountPendingUseCase(private val tasks: List<PendingSyncTask>) {
    suspend operator fun invoke(): Int = tasks.sumOf { it.pendingCount() }
}

/** Pendientes de un grupo (ej. 2 vehículos sin enviar). */
data class PendingSummaryItem(val kind: PendingSyncKind, val count: Int)

/**
 * Pendientes de toda la app agrupados por [PendingSyncKind] (solo los grupos con algo).
 * Cada feature guarda en su propio archivo, así que se vuelve a contar cada [intervalMillis]
 * mientras alguien escucha (contar es leer la copia en memoria: es barato).
 */
class ObservePendingSummaryUseCase(private val tasks: List<PendingSyncTask>) {
    operator fun invoke(intervalMillis: Long = DEFAULT_INTERVAL_MS): Flow<List<PendingSummaryItem>> = flow {
        while (true) {
            emit(summary())
            delay(intervalMillis)
        }
    }.distinctUntilChanged()

    suspend fun summary(): List<PendingSummaryItem> =
        tasks.groupBy { it.kind }
            .map { (kind, group) -> PendingSummaryItem(kind, group.sumOf { it.pendingCount() }) }
            .filter { it.count > 0 }
            .sortedBy { it.kind.ordinal }

    private companion object {
        const val DEFAULT_INTERVAL_MS = 5_000L
    }
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

/**
 * true si el error significa "sin red / sesión terminada": se detiene y se reintenta después.
 *
 * Con el refresh token, un 401 ya NO llega aquí por un JWT vencido: el AuthPlugin renueva la sesión y
 * reintenta la petición solo. Así que:
 * - [AppError.Unauthorized] = la API rechazó también el refresh token (sesión revocada o vencida);
 *   la app ya cerró la sesión y no tiene caso seguir.
 * - [AppError.NoConnection] = sin red, o el JWT venció y no hubo red para renovarlo
 *   (SessionRefreshUnavailableException): la sesión sigue y se reintenta al volver la conexión.
 */
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

/**
 * Ejecuta todas las [PendingSyncTask] registradas, en el orden de [PendingSyncKind]
 * (viajes y recargas primero). Se detiene en el primer "sin red / sesión vencida".
 */
class SyncAllPendingUseCase(private val tasks: List<PendingSyncTask>) {
    suspend operator fun invoke(): AppResult<Unit> {
        for (task in tasks.sortedBy { it.kind.ordinal }) {
            val result = task.syncPending()
            if (result is AppResult.Failure && result.error.stopsSync()) return result
        }
        return AppResult.Success(Unit)
    }
}
