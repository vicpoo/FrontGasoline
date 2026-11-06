package org.vicpoo.gasolineproject.domain.sync

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pendientes de toda la app: orden de envío y resumen para "Envíos pendientes". */
class PendingSummaryTest {

    private class FakeTask(
        override val kind: PendingSyncKind,
        private val pending: Int = 0,
        private val result: AppResult<Unit> = AppResult.Success(Unit),
        private val calls: MutableList<PendingSyncKind>? = null,
    ) : PendingSyncTask {
        override suspend fun syncPending(): AppResult<Unit> {
            calls?.add(kind)
            return result
        }

        override suspend fun pendingCount(): Int = pending
    }

    @Test
    fun losViajesSeEnvianPrimero_aunqueSeRegistrenAlFinal() = runTest {
        val calls = mutableListOf<PendingSyncKind>()
        val tasks = listOf(
            FakeTask(PendingSyncKind.DEVICE, calls = calls),
            FakeTask(PendingSyncKind.VEHICLES, calls = calls),
            FakeTask(PendingSyncKind.RESERVATIONS, calls = calls),
            FakeTask(PendingSyncKind.TRIPS, calls = calls),
        )

        SyncAllPendingUseCase(tasks)()

        assertEquals(
            listOf(PendingSyncKind.TRIPS, PendingSyncKind.RESERVATIONS, PendingSyncKind.VEHICLES, PendingSyncKind.DEVICE),
            calls,
        )
    }

    @Test
    fun sinRedEnLosViajes_noSigueConLoDemas() = runTest {
        val calls = mutableListOf<PendingSyncKind>()
        val tasks = listOf(
            FakeTask(PendingSyncKind.VEHICLES, calls = calls),
            FakeTask(PendingSyncKind.TRIPS, result = AppResult.Failure(AppError.NoConnection), calls = calls),
        )

        val result = SyncAllPendingUseCase(tasks)()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        assertEquals(listOf(PendingSyncKind.TRIPS), calls)
    }

    @Test
    fun resumen_agrupaPorTipo_yOmiteLosQueNoTienenNada() = runTest {
        val tasks = listOf(
            FakeTask(PendingSyncKind.VEHICLES, pending = 2),
            FakeTask(PendingSyncKind.DEVICE, pending = 0),
            FakeTask(PendingSyncKind.TRIPS, pending = 1),
            FakeTask(PendingSyncKind.RESERVATIONS, pending = 3),
        )

        val summary = ObservePendingSummaryUseCase(tasks).summary()

        assertEquals(
            listOf(
                PendingSummaryItem(PendingSyncKind.TRIPS, 1),
                PendingSummaryItem(PendingSyncKind.RESERVATIONS, 3),
                PendingSummaryItem(PendingSyncKind.VEHICLES, 2),
            ),
            summary,
        )
        assertEquals(6, CountPendingUseCase(tasks)())
    }
}
