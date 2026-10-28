package org.vicpoo.gasolineproject.feature.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.data.LastRefreshStore
import org.vicpoo.gasolineproject.core.network.ConnectivityMonitor
import org.vicpoo.gasolineproject.core.network.reconnections
import org.vicpoo.gasolineproject.feature.reports.domain.DownloadReportUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.FleetReport
import org.vicpoo.gasolineproject.feature.reports.domain.ObserveReportUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.ObserveReportVehiclesUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.RefreshReportUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.ReportFile
import org.vicpoo.gasolineproject.feature.reports.domain.ReportFilters
import org.vicpoo.gasolineproject.feature.reports.domain.ReportFormat
import org.vicpoo.gasolineproject.feature.reports.domain.ReportPeriod
import org.vicpoo.gasolineproject.feature.reports.domain.ReportVehicleOption

data class ReportsUiState(
    val filters: ReportFilters = ReportFilters(),
    val report: FleetReport? = null,
    /** El reporte que se ve es de otros filtros (sin red no se pudo pedir el nuevo). */
    val showingOtherFilters: Boolean = false,
    val vehicles: List<ReportVehicleOption> = emptyList(),
    val isInitialLoading: Boolean = true,
    /** Se está pidiendo el reporte (al cambiar un filtro o al jalar para recargar). */
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: AppError? = null,
    val lastUpdatedAtMillis: Long? = null,
    /** Formato que se está descargando (null = ninguno). */
    val downloading: ReportFormat? = null,
    val downloadError: AppError? = null,
) {
    /** Los archivos los genera la API: sin red no se pueden descargar. */
    val canDownload: Boolean get() = !isOffline && downloading == null
}

sealed interface ReportsEvent {
    data class PeriodSelected(val period: ReportPeriod) : ReportsEvent

    /** null = toda la flota. */
    data class VehicleSelected(val vehicleId: String?) : ReportsEvent
    data object Refresh : ReportsEvent
    data class Download(val format: ReportFormat) : ReportsEvent
    data object DismissDownloadError : ReportsEvent
}

sealed interface ReportsEffect {
    /** Abrir el menú de compartir del sistema con el archivo descargado. */
    data class Share(val file: ReportFile) : ReportsEffect
}

class ReportsViewModel(
    observeReport: ObserveReportUseCase,
    observeVehicles: ObserveReportVehiclesUseCase,
    private val refreshReport: RefreshReportUseCase,
    private val downloadReport: DownloadReportUseCase,
    /** Se recarga solo al volver la red y marca "sin conexión" en cuanto se pierde. */
    private val connectivity: ConnectivityMonitor,
    private val lastRefresh: LastRefreshStore,
) : ViewModel() {

    private data class Status(
        val filters: ReportFilters = ReportFilters(),
        val loadedOnce: Boolean = false,
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val isOffline: Boolean = false,
        val error: AppError? = null,
        val downloading: ReportFormat? = null,
        val downloadError: AppError? = null,
    )

    private val status = MutableStateFlow(Status())
    private var loadJob: Job? = null

    private val _effects = Channel<ReportsEffect>(Channel.BUFFERED)
    val effects: Flow<ReportsEffect> = _effects.receiveAsFlow()

    val state: StateFlow<ReportsUiState> = combine(
        observeReport(),
        observeVehicles(),
        status,
        connectivity.isOnline,
        lastRefresh.observe(REFRESH_KEY),
    ) { saved, vehicles, s, online, lastUpdated ->
        ReportsUiState(
            filters = s.filters,
            report = saved?.report,
            showingOtherFilters = saved != null && saved.filters != s.filters,
            vehicles = vehicles,
            isInitialLoading = saved == null && !s.loadedOnce,
            isRefreshing = s.isRefreshing,
            isOffline = s.isOffline || !online,
            error = s.error,
            lastUpdatedAtMillis = lastUpdated,
            downloading = s.downloading,
            downloadError = s.downloadError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    init {
        load(showIndicator = false)
        viewModelScope.launch { connectivity.reconnections().collect { load(showIndicator = false) } }
    }

    fun onEvent(event: ReportsEvent) {
        when (event) {
            is ReportsEvent.PeriodSelected -> changeFilters(status.value.filters.copy(period = event.period))
            is ReportsEvent.VehicleSelected -> changeFilters(status.value.filters.copy(vehicleId = event.vehicleId))
            ReportsEvent.Refresh -> load(showIndicator = true)
            is ReportsEvent.Download -> download(event.format)
            ReportsEvent.DismissDownloadError -> status.update { it.copy(downloadError = null) }
        }
    }

    /** Al volver a la pestaña: se actualiza sin mostrar el indicador. */
    fun refreshSilently() = load(showIndicator = false)

    private fun changeFilters(filters: ReportFilters) {
        if (filters == status.value.filters) return
        status.update { it.copy(filters = filters) }
        // Cambiar un filtro sí muestra el indicador: el usuario espera ver otros números
        load(showIndicator = true, force = true)
    }

    private fun load(showIndicator: Boolean, force: Boolean = false) {
        if (status.value.isLoading && !force) {
            if (showIndicator) status.update { it.copy(isRefreshing = true) }
            return
        }
        // Al cambiar de filtro se cancela la petición anterior: su respuesta ya no sirve
        if (force) loadJob?.cancel()
        val filters = status.value.filters
        loadJob = viewModelScope.launch {
            status.update { it.copy(isLoading = true, isRefreshing = showIndicator) }
            val failure = (refreshReport(filters) as? AppResult.Failure)?.error
            // Si mientras tanto cambiaron los filtros, esta respuesta ya no manda
            if (status.value.filters != filters) return@launch
            if (failure == null) lastRefresh.markNow(REFRESH_KEY)
            status.update {
                it.copy(
                    loadedOnce = true,
                    isLoading = false,
                    isRefreshing = false,
                    isOffline = failure == AppError.NoConnection,
                    error = failure?.takeIf { e -> e != AppError.NoConnection },
                )
            }
        }
    }

    private fun download(format: ReportFormat) {
        if (status.value.downloading != null) return
        val filters = status.value.filters
        viewModelScope.launch {
            status.update { it.copy(downloading = format, downloadError = null) }
            when (val result = downloadReport(filters, format)) {
                is AppResult.Success -> {
                    status.update { it.copy(downloading = null) }
                    _effects.send(ReportsEffect.Share(result.data))
                }
                is AppResult.Failure -> status.update {
                    it.copy(
                        downloading = null,
                        isOffline = it.isOffline || result.error == AppError.NoConnection,
                        downloadError = result.error,
                    )
                }
            }
        }
    }

    private companion object {
        /** Llave de "Actualizado hace X" de esta pantalla. */
        const val REFRESH_KEY = "reports.fleet"
    }
}
