package org.vicpoo.gasolineproject.ui.reports

import org.vicpoo.gasolineproject.domain.reports.ReportFormat
import org.vicpoo.gasolineproject.domain.reports.ReportPeriod

sealed interface ReportsEvent {
    data class PeriodSelected(val period: ReportPeriod) : ReportsEvent

    /** Fechas elegidas en el calendario ("yyyy-MM-dd", ambos días incluidos). */
    data class CustomRangeSelected(val from: String, val to: String) : ReportsEvent

    /** null = toda la flota. */
    data class VehicleSelected(val vehicleId: String?) : ReportsEvent
    data object Refresh : ReportsEvent
    data class Download(val format: ReportFormat) : ReportsEvent
    data object DismissDownloadError : ReportsEvent
}
