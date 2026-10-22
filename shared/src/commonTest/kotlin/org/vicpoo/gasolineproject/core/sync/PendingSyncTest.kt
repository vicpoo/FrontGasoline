package org.vicpoo.gasolineproject.core.sync

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingSyncTest {

    private class RecordingTask(private val result: AppResult<Unit>) : PendingSyncTask {
        var calls = 0
        override suspend fun syncPending(): AppResult<Unit> {
            calls++
            return result
        }
    }

    @Test
    fun clientErrors_arePermanent() {
        assertTrue(SyncRejection.isPermanent(400))
        assertTrue(SyncRejection.isPermanent(409))
        assertTrue(SyncRejection.isPermanent(SyncRejection.LOCAL_INVALID))
    }

    @Test
    fun serverAndThrottlingErrors_areRetried() {
        assertFalse(SyncRejection.isPermanent(500))
        assertFalse(SyncRejection.isPermanent(503))
        assertFalse(SyncRejection.isPermanent(408))
        assertFalse(SyncRejection.isPermanent(429))
        assertFalse(SyncRejection.isPermanent(null))
    }

    @Test
    fun stopsSync_onlyForNoConnectionOrExpiredSession() {
        assertTrue(AppError.NoConnection.stopsSync())
        assertTrue(AppError.Unauthorized.stopsSync())
        assertFalse(AppError.Server(400, "x").stopsSync())
    }

    @Test
    fun syncAll_runsEveryTask() = runTest {
        val first = RecordingTask(AppResult.Success(Unit))
        val second = RecordingTask(AppResult.Success(Unit))

        val result = SyncAllPendingUseCase(listOf(first, second))()

        assertTrue(result is AppResult.Success)
        assertEquals(1, first.calls)
        assertEquals(1, second.calls)
    }

    @Test
    fun syncAll_stopsWithoutConnection() = runTest {
        val offline = RecordingTask(AppResult.Failure(AppError.NoConnection))
        val next = RecordingTask(AppResult.Success(Unit))

        val result = SyncAllPendingUseCase(listOf(offline, next))()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        assertEquals(0, next.calls)
    }

    @Test
    fun syncAll_continuesAfterARejection() = runTest {
        val rejected = RecordingTask(AppResult.Failure(AppError.Server(500, null)))
        val next = RecordingTask(AppResult.Success(Unit))

        val result = SyncAllPendingUseCase(listOf(rejected, next))()

        assertTrue(result is AppResult.Success)
        assertEquals(1, next.calls)
    }
}
