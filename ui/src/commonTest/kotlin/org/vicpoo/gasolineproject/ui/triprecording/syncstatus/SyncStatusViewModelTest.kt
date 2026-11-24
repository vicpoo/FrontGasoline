package org.vicpoo.gasolineproject.ui.triprecording.syncstatus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.sync.PendingSummaryItem
import org.vicpoo.gasolineproject.domain.sync.PendingSyncKind
import org.vicpoo.gasolineproject.domain.sync.usecase.ObservePendingSummaryUseCase
import org.vicpoo.gasolineproject.domain.sync.usecase.SyncAllPendingUseCase
import org.vicpoo.gasolineproject.domain.triprecording.PendingFuelStatus
import org.vicpoo.gasolineproject.domain.triprecording.PendingTripState
import org.vicpoo.gasolineproject.domain.triprecording.PendingTripStatus
import org.vicpoo.gasolineproject.domain.triprecording.RecordSyncState
import org.vicpoo.gasolineproject.domain.triprecording.SyncStatus
import org.vicpoo.gasolineproject.domain.triprecording.SyncStatusRepository
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ObserveSyncStatusUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.SyncNowUseCase
import org.vicpoo.gasolineproject.testing.FakePendingSyncTask
import org.vicpoo.gasolineproject.testing.MainDispatcherTest

/**
 * "Envíos pendientes": resumen de lo que falta, número del aviso y "Enviar ahora".
 * Ojo: el resumen de pendientes se vuelve a contar cada cierto tiempo (bucle con delay), así que
 * aquí se usa runCurrent() y nunca advanceUntilIdle() (no terminaría).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncStatusViewModelTest : MainDispatcherTest() {

    private class FakeSyncStatusRepository : SyncStatusRepository {
        val status = MutableStateFlow(SyncStatus())
        override fun observe() = status
    }

    private val statusRepository = FakeSyncStatusRepository()
    private val trips = FakePendingSyncTask(PendingSyncKind.TRIPS, count = 3)
    private val reservations = FakePendingSyncTask(PendingSyncKind.RESERVATIONS, count = 2)
    private val tasks = listOf(trips, reservations)

    private fun TestScope.viewModel(): SyncStatusViewModel {
        val vm = SyncStatusViewModel(
            observeStatus = ObserveSyncStatusUseCase(statusRepository),
            observePendingSummary = ObservePendingSummaryUseCase(tasks),
            syncNow = SyncNowUseCase(SyncAllPendingUseCase(tasks), AppScope(backgroundScope)),
        )
        // El estado solo se calcula mientras alguien lo escucha (WhileSubscribed)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    private fun tripWithFuel(state: PendingTripState, vararg fuel: RecordSyncState) = PendingTripStatus(
        localId = "trip-1",
        vehicleTitle = "Nissan Versa",
        plates = "ABC-123",
        startedAt = "2026-11-23T08:00:00-06:00",
        state = state,
        registeredOnServer = true,
        closedOnServer = false,
        errorMessage = null,
        pointsPending = 0,
        pausesPending = 0,
        odometerPhotoPending = false,
        fuel = fuel.mapIndexed { i, s ->
            PendingFuelStatus(localId = "fuel-$i", number = i + 1, liters = 20.0, amount = 500.0, state = s, errorMessage = null)
        },
    )

    @Test
    fun losViajesVienenDelDiagnostico_yElRestoDelResumen_sinRepetirlos() = runTest {
        statusRepository.status.value = SyncStatus(
            trips = listOf(tripWithFuel(PendingTripState.WAITING_TO_SEND, RecordSyncState.PENDING, RecordSyncState.SYNCED)),
        )

        val vm = viewModel()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(listOf(PendingSummaryItem(PendingSyncKind.RESERVATIONS, 2)), state.others)
        // 1 recarga sin enviar + 1 viaje esperando + 2 solicitudes
        assertEquals(4, state.pendingCount)
        assertFalse(state.isAllSent)
    }

    @Test
    fun sinNadaPendiente_todoEnviado() = runTest {
        trips.count = 0
        reservations.count = 0

        val vm = viewModel()

        assertTrue(vm.state.value.isAllSent)
        assertEquals(0, vm.state.value.pendingCount)
    }

    @Test
    fun enviarAhora_conExito_muestraListo() = runTest {
        val vm = viewModel()

        vm.onEvent(SyncStatusEvent.SendNowClicked)
        assertTrue(vm.state.value.isSending)
        runCurrent()

        assertEquals(SendNowResult.DONE, vm.state.value.sendResult)
        assertFalse(vm.state.value.isSending)
        assertEquals(1, trips.syncCalls)
        assertEquals(1, reservations.syncCalls)
    }

    @Test
    fun enviarAhora_sinRed_muestraSinConexion() = runTest {
        trips.result = AppResult.Failure(AppError.NoConnection)
        val vm = viewModel()

        vm.onEvent(SyncStatusEvent.SendNowClicked)
        runCurrent()

        assertEquals(SendNowResult.OFFLINE, vm.state.value.sendResult)
        assertEquals(0, reservations.syncCalls, "Sin red se detiene todo")
    }

    @Test
    fun enviarAhora_sesionVencida_muestraError() = runTest {
        trips.result = AppResult.Failure(AppError.Unauthorized)
        val vm = viewModel()

        vm.onEvent(SyncStatusEvent.SendNowClicked)
        runCurrent()

        assertEquals(SendNowResult.ERROR, vm.state.value.sendResult)
    }

    @Test
    fun cerrarElResultado_loQuita() = runTest {
        val vm = viewModel()
        vm.onEvent(SyncStatusEvent.SendNowClicked)
        runCurrent()

        vm.onEvent(SyncStatusEvent.ResultDismissed)

        assertNull(vm.state.value.sendResult)
    }

    @Test
    fun siLaSincronizacionAutomaticaEstaCorriendo_seMuestraEnviando() = runTest {
        val vm = viewModel()

        statusRepository.status.value = SyncStatus(isSyncing = true)

        assertTrue(vm.state.value.isSending)
    }

    @Test
    fun bitacoraCompleta_seAbreYSeCierra() = runTest {
        val vm = viewModel()

        vm.onEvent(SyncStatusEvent.ToggleFullLog)
        assertTrue(vm.state.value.showFullLog)

        vm.onEvent(SyncStatusEvent.ToggleFullLog)
        assertFalse(vm.state.value.showFullLog)
    }
}
