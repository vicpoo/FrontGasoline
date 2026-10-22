package org.vicpoo.gasolineproject.feature.triprecording.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.platform.PickedFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RetryFuelRecordUseCaseTest {

    private fun record(state: RecordSyncState, message: String? = null) = FuelRecord(
        localId = "fuel-1",
        serverId = null,
        tripLocalId = "trip-1",
        number = 1,
        amount = 500.0,
        liters = 20.0,
        costCenter = null,
        receipt = null,
        loadedAt = "2026-10-19T10:00:00-06:00",
        location = null,
        syncState = state,
        syncErrorMessage = message,
    )

    /** Repositorio en memoria: guarda en qué orden se llamó. */
    private class FakeFuelRepository(var current: FuelRecord?) : FuelRecordRepository {
        val calls = mutableListOf<String>()
        override fun observeForTrip(tripLocalId: String): Flow<List<FuelRecord>> = flowOf(listOfNotNull(current))
        override suspend fun get(localId: String): FuelRecord? = current
        override suspend fun save(
            tripLocalId: String,
            localId: String?,
            values: FuelRecordValues,
            newReceipt: PickedFile?,
        ): AppResult<FuelRecord> = error("No se usa en esta prueba")
        override suspend fun delete(localId: String): AppResult<Unit> = error("No se usa en esta prueba")
        override suspend fun markForRetry(localId: String) {
            calls += "markForRetry"
            current = current?.copy(syncState = RecordSyncState.PENDING, syncErrorMessage = null)
        }
    }

    /** Sincronización falsa: aplica [onSync] al repositorio y regresa [result]. */
    private class FakeSync(
        private val repository: FakeFuelRepository,
        private val result: AppResult<Unit>,
        private val onSync: (FuelRecord) -> FuelRecord,
    ) : TripRecordingSync {
        override suspend fun sync(): AppResult<Unit> {
            repository.calls += "sync"
            repository.current = repository.current?.let(onSync)
            return result
        }
        override fun requestSync() = Unit
    }

    @Test
    fun clearsTheRejectionBeforeSending() = runTest {
        val repository = FakeFuelRepository(record(RecordSyncState.ERROR, "Fuera del periodo"))
        val sync = FakeSync(repository, AppResult.Success(Unit)) { it.copy(syncState = RecordSyncState.SYNCED) }

        val outcome = RetryFuelRecordUseCase(repository, sync)("fuel-1")

        assertEquals(listOf("markForRetry", "sync"), repository.calls)
        assertEquals(SendOutcome.Sent, outcome)
    }

    @Test
    fun rejectedAgain_reportsTheServerMessage() = runTest {
        val repository = FakeFuelRepository(record(RecordSyncState.ERROR, "x"))
        val sync = FakeSync(repository, AppResult.Success(Unit)) {
            it.copy(syncState = RecordSyncState.ERROR, syncErrorMessage = "Litros mayores al tanque")
        }

        val outcome = RetryFuelRecordUseCase(repository, sync)("fuel-1")

        assertEquals(SendOutcome.Rejected("Litros mayores al tanque"), outcome)
    }

    @Test
    fun withoutConnection_staysSavedOffline() = runTest {
        val repository = FakeFuelRepository(record(RecordSyncState.ERROR, "x"))
        val sync = FakeSync(repository, AppResult.Failure(AppError.NoConnection)) { it }

        val outcome = RetryFuelRecordUseCase(repository, sync)("fuel-1")

        assertTrue(outcome is SendOutcome.SavedOffline)
    }
}
