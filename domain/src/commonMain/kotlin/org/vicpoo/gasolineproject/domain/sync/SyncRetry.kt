package org.vicpoo.gasolineproject.domain.sync

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/*
 * Política de reintentos de la sincronización en segundo plano (punto 3.3.1).
 *
 * - Sin red: se detiene todo y el planificador reintenta con backoff exponencial cuando hay red.
 * - Error temporal del servidor (5xx, 408, 429): el registro se queda pendiente y la vuelta COMPLETA se
 *   vuelve a programar con backoff exponencial; si la API mandó Retry-After, se espera ese tiempo.
 * - Rechazo definitivo (4xx): no se reintenta solo (ver SyncRejection).
 *
 * No se reintenta la misma petición en el momento: casi todos los envíos son POST que crean algo
 * (viaje, carga, vehículo). Si el servidor falló DESPUÉS de guardarlo, repetirlo de inmediato puede
 * duplicarlo; esperar a la siguiente vuelta da tiempo a que el servidor se recupere.
 */

/** 5xx, 408 (timeout del servidor) y 429 (demasiadas peticiones): vale la pena reintentar más tarde. */
fun isTemporaryHttpStatus(code: Int): Boolean = code >= 500 || code == 408 || code == 429

/**
 * Anota los errores temporales que hubo durante UNA vuelta de sincronización. Viaja en el contexto de la
 * corrutina ([RunPendingSyncUseCase] lo pone), así safeApiCall lo encuentra sin que cada repositorio lo reciba.
 */
class TemporarySyncFailures : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<TemporarySyncFailures>

    private val mutex = Mutex()

    var count: Int = 0
        private set

    /** El Retry-After más largo que pidió la API (null si ninguna respuesta lo mandó). */
    var maxRetryAfterMillis: Long? = null
        private set

    suspend fun record(retryAfterMillis: Long?) = mutex.withLock {
        count++
        if (retryAfterMillis != null) maxRetryAfterMillis = maxOf(maxRetryAfterMillis ?: 0L, retryAfterMillis)
    }
}

/** Lo llama safeApiCall ante un error temporal. Fuera de una vuelta de sincronización no hace nada. */
suspend fun recordTemporarySyncFailure(retryAfterMillis: Long? = null) {
    currentCoroutineContext()[TemporarySyncFailures]?.record(retryAfterMillis)
}

/** Qué hacer después de una vuelta de sincronización en segundo plano. */
sealed interface SyncRunOutcome {
    /** Se mandó todo lo que se podía (los rechazos definitivos ya quedaron en cada registro). */
    data object Done : SyncRunOutcome

    /** Sin red o error temporal: volver a intentar con backoff exponencial. */
    data object RetryWithBackoff : SyncRunOutcome

    /** La API pidió esperar (Retry-After): volver a intentar después de [delayMillis]. */
    data class RetryAfter(val delayMillis: Long) : SyncRunOutcome
}

/** Una vuelta de sincronización en segundo plano y la decisión de reintento (la usa el Worker). */
class RunPendingSyncUseCase(private val syncAll: SyncAllPendingUseCase) {
    suspend operator fun invoke(): SyncRunOutcome {
        val failures = TemporarySyncFailures()
        val result = withContext(failures) { syncAll() }
        return when {
            result is AppResult.Failure && result.error == AppError.NoConnection -> SyncRunOutcome.RetryWithBackoff
            // La sesión terminó: no tiene caso reintentar hasta que vuelva a entrar
            result is AppResult.Failure && result.error == AppError.Unauthorized -> SyncRunOutcome.Done
            failures.count > 0 -> failures.maxRetryAfterMillis?.let { SyncRunOutcome.RetryAfter(it) }
                ?: SyncRunOutcome.RetryWithBackoff
            else -> SyncRunOutcome.Done
        }
    }
}
