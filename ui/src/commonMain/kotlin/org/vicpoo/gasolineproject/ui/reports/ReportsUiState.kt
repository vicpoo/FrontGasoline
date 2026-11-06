package org.vicpoo.gasolineproject.ui.reports

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.reports.FleetReport
import org.vicpoo.gasolineproject.domain.reports.ReportFilters
import org.vicpoo.gasolineproject.domain.reports.ReportFormat
import org.vicpoo.gasolineproject.domain.reports.ReportVehicleOption

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
