package org.vicpoo.gasolineproject.feature.reports.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.feature.reports.domain.DownloadReportUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.FleetReport
import org.vicpoo.gasolineproject.feature.reports.domain.ObserveReportUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.ObserveReportVehiclesUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.RefreshReportUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.ReportFile
import org.vicpoo.gasolineproject.feature.reports.domain.ReportFilters
import org.vicpoo.gasolineproject.feature.reports.domain.ReportFormat
import org.vicpoo.gasolineproject.feature.reports.domain.ReportPeriod
import org.vicpoo.gasolineproject.feature.reports.domain.ReportSummary
import org.vicpoo.gasolineproject.feature.reports.domain.ReportVehicleOption
import org.vicpoo.gasolineproject.feature.reports.domain.ReportsRepository
import org.vicpoo.gasolineproject.feature.reports.domain.SavedReport
import org.vicpoo.gasolineproject.testing.FakeConnectivityMonitor
import org.vicpoo.gasolineproject.testing.FakeLastRefreshStore
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Reportes: filtros, modo sin conexión y descarga de archivos. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest : MainDispatcherTest() {

    /** Repositorio en memoria: guarda el reporte con los filtros con que se pidió. */
    private class FakeReportsRepository : ReportsRepository {
        val saved = MutableStateFlow<SavedReport?>(null)
        var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
        var downloadResult: AppResult<ReportFile> = AppResult.Success(ReportFile("r.pdf", "application/pdf", byteArrayOf(1)))
        val requested = mutableListOf<ReportFilters>()
        val downloads = mutableListOf<Pair<ReportFilters, ReportFormat>>()

        override fun observeReport(): Flow<SavedReport?> = saved
        override fun observeVehicles(): Flow<List<ReportVehicleOption>> = MutableStateFlow(emptyList())

        override suspend fun refresh(filters: ReportFilters): AppResult<Unit> {
            requested += filters
            if (refreshResult is AppResult.Success) saved.value = SavedReport(filters, report())
            return refreshResult
        }

        override suspend fun download(filters: ReportFilters, format: ReportFormat): AppResult<ReportFile> {
            downloads += filters to format
            return downloadResult
        }
    }

    private val repository = FakeReportsRepository()
    private val network = FakeConnectivityMonitor(initial = true)
    private val lastRefresh = FakeLastRefreshStore(now = { 42L })

    private fun viewModel() = ReportsViewModel(
        observeReport = ObserveReportUseCase(repository),
        observeVehicles = ObserveReportVehiclesUseCase(repository),
        refreshReport = RefreshReportUseCase(repository),
        downloadReport = DownloadReportUseCase(repository),
        connectivity = network,
        lastRefresh = lastRefresh,
    )

    @Test
    fun alAbrir_pideElReporteDeEsteMesDeTodaLaFlota() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertEquals(listOf(ReportFilters(ReportPeriod.THIS_MONTH, null)), repository.requested)
        assertEquals(42L, vm.state.value.lastUpdatedAtMillis)
        assertFalse(vm.state.value.showingOtherFilters)
    }

    @Test
    fun alCambiarPeriodoOCarro_seVuelveAPedir() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onEvent(ReportsEvent.PeriodSelected(ReportPeriod.THIS_YEAR))
        vm.onEvent(ReportsEvent.VehicleSelected("car-1"))

        assertEquals(ReportFilters(ReportPeriod.THIS_YEAR, "car-1"), repository.requested.last())
        assertEquals(3, repository.requested.size)
    }

    @Test
    fun sinRed_seVeElUltimoGuardado_yAvisaQueEsDeOtrosFiltros() = runTest {
        repository.saved.value = SavedReport(ReportFilters(ReportPeriod.LAST_MONTH), report())
        repository.refreshResult = AppResult.Failure(AppError.NoConnection)
        network.online.value = false
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        val state = vm.state.value
        assertTrue(state.isOffline)
        assertNull(state.error, "Sin red no es un error: se muestra el aviso")
        assertTrue(state.showingOtherFilters)
        assertFalse(state.canDownload, "Los archivos los genera la API: sin red no se descargan")
    }

    @Test
    fun alVolverLaRed_recargaSolo() = runTest {
        network.online.value = false
        repository.refreshResult = AppResult.Failure(AppError.NoConnection)
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        repository.refreshResult = AppResult.Success(Unit)
        network.online.value = true

        assertEquals(2, repository.requested.size)
        assertFalse(vm.state.value.isOffline)
    }

    @Test
    fun descargar_compartirElArchivoConLosFiltrosActuales() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.onEvent(ReportsEvent.PeriodSelected(ReportPeriod.LAST_MONTH))

        vm.onEvent(ReportsEvent.Download(ReportFormat.EXCEL))

        val effect = vm.effects.first()
        assertIs<ReportsEffect.Share>(effect)
        assertEquals(ReportFilters(ReportPeriod.LAST_MONTH) to ReportFormat.EXCEL, repository.downloads.single())
        assertNull(vm.state.value.downloading)
    }

    @Test
    fun descargaFallida_muestraElError() = runTest {
        repository.downloadResult = AppResult.Failure(AppError.Forbidden("Sin acceso"))
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onEvent(ReportsEvent.Download(ReportFormat.PDF))

        assertEquals(AppError.Forbidden("Sin acceso"), vm.state.value.downloadError)
        vm.onEvent(ReportsEvent.DismissDownloadError)
        assertNull(vm.state.value.downloadError)
    }

    private companion object {
        fun report() = FleetReport(
            companyName = "Empresa",
            from = null,
            to = null,
            generatedAt = "2026-10-28T10:00:00",
            summary = ReportSummary(1, 1, 2, 120.0, 118.0, 1, 10.0, 250.0, 25.0, 12.0, 2.08),
            vehicles = emptyList(),
        )
    }
}
