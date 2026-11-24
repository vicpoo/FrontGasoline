package org.vicpoo.gasolineproject.domain.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.sync.usecase.PendingBeforeLogoutUseCase

/** Antes de "Salir": se intenta mandar lo pendiente y se avisa si algo se perdería. */
class PendingBeforeLogoutTest {

    /** Tarea falsa: [sendsOk] = la sincronización vacía sus pendientes. */
    private class FakeTask(var pending: Int, private val sendsOk: Boolean) : PendingSyncTask {
        var syncCalls = 0
        override suspend fun syncPending(): AppResult<Unit> {
            syncCalls++
            return if (sendsOk) {
                pending = 0
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.NoConnection)
            }
        }
        override suspend fun pendingCount(): Int = pending
    }

    @Test
    fun sinPendientes_noSincroniza_yRegresaCero() = runTest {
        val task = FakeTask(pending = 0, sendsOk = true)

        assertEquals(0, PendingBeforeLogoutUseCase(listOf(task))())
        assertEquals(0, task.syncCalls)
    }

    @Test
    fun conPendientes_yRed_seMandan_yRegresaCero() = runTest {
        val task = FakeTask(pending = 3, sendsOk = true)

        assertEquals(0, PendingBeforeLogoutUseCase(listOf(task))())
        assertEquals(1, task.syncCalls)
    }

    @Test
    fun conPendientes_sinRed_regresaCuantosSePerderian() = runTest {
        val trips = FakeTask(pending = 2, sendsOk = false)
        val vehicles = FakeTask(pending = 1, sendsOk = false)

        assertEquals(3, PendingBeforeLogoutUseCase(listOf(trips, vehicles))())
    }

    /** Viajes: se intentan mandar, pero no se borran al salir (se guardan para esa cuenta). */
    private class KeptTask(var pending: Int) : PendingSyncTask {
        var syncCalls = 0
        override suspend fun syncPending(): AppResult<Unit> {
            syncCalls++
            return AppResult.Failure(AppError.NoConnection)
        }
        override suspend fun pendingCount(): Int = pending
        override suspend fun lostOnLogoutCount(): Int = 0
    }

    @Test
    fun viajeEnCurso_seIntentaMandar_peroNoCuentaComoPerdido() = runTest {
        val trips = KeptTask(pending = 1)

        assertEquals(0, PendingBeforeLogoutUseCase(listOf(trips))())
        assertEquals(1, trips.syncCalls)
    }

    @Test
    fun viajeEnCurso_masOtrosSinEnviar_soloCuentanLosQueSeBorran() = runTest {
        val trips = KeptTask(pending = 2)
        val reservations = FakeTask(pending = 1, sendsOk = false)

        assertEquals(1, PendingBeforeLogoutUseCase(listOf(trips, reservations))())
    }
}
