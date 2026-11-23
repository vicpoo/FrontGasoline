package org.vicpoo.gasolineproject.domain.sync

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Política de reintentos en segundo plano: sin red, errores temporales, Retry-After y sesión terminada. */
class RunPendingSyncUseCaseTest {

    /** Tarea que, al enviar, "recibe" de la API lo que diga [onSync] (ej. un 503). */
    private class Task(
        override val kind: PendingSyncKind = PendingSyncKind.OTHER,
        private val result: AppResult<Unit> = AppResult.Success(Unit),
        private val onSync: suspend () -> Unit = {},
    ) : PendingSyncTask {
        override suspend fun syncPending(): AppResult<Unit> {
            onSync()
            return result
        }
    }

    private suspend fun run(vararg tasks: Task) = RunPendingSyncUseCase(SyncAllPendingUseCase(tasks.toList()))()

    @Test
    fun todoBien_termina() = runTest {
        assertEquals(SyncRunOutcome.Done, run(Task(), Task()))
    }

    @Test
    fun sinRed_reintentaConBackoff() = runTest {
        assertEquals(SyncRunOutcome.RetryWithBackoff, run(Task(result = AppResult.Failure(AppError.NoConnection))))
    }

    @Test
    fun sesionTerminada_noReintenta() = runTest {
        assertEquals(SyncRunOutcome.Done, run(Task(result = AppResult.Failure(AppError.Unauthorized))))
    }

    @Test
    fun errorTemporalDelServidor_reintentaConBackoff_aunqueLaTareaTermineBien() = runTest {
        // Ej. un 503 en un lote de la ruta: el registro se queda pendiente y la tarea sigue con lo demás
        val outcome = run(Task(onSync = { recordTemporarySyncFailure() }), Task())

        assertEquals(SyncRunOutcome.RetryWithBackoff, outcome)
    }

    @Test
    fun conRetryAfter_esperaLoQuePidioLaApi_elMasLargo() = runTest {
        val outcome = run(
            Task(onSync = { recordTemporarySyncFailure(retryAfterMillis = 30_000) }),
            Task(onSync = { recordTemporarySyncFailure(retryAfterMillis = 120_000) }),
            Task(onSync = { recordTemporarySyncFailure() }),
        )

        assertEquals(SyncRunOutcome.RetryAfter(120_000), outcome)
    }

    @Test
    fun losErroresEnParalelo_tambienSeCuentan() = runTest {
        val outcome = run(
            Task(onSync = {
                coroutineScope { List(3) { async { recordTemporarySyncFailure() } }.awaitAll() }
            }),
        )

        assertEquals(SyncRunOutcome.RetryWithBackoff, outcome)
    }

    @Test
    fun fueraDeUnaVuelta_anotarNoHaceNada() = runTest {
        recordTemporarySyncFailure(retryAfterMillis = 1_000) // ej. una pantalla con un 503: no debe fallar
    }

    @Test
    fun soloSonTemporales_5xx_408_y_429() {
        listOf(500, 502, 503, 504, 408, 429).forEach { assertTrue(isTemporaryHttpStatus(it), "$it") }
        listOf(400, 401, 403, 404, 409, 422).forEach { assertFalse(isTemporaryHttpStatus(it), "$it") }
    }
}
