package org.vicpoo.gasolineproject.feature.reports.domain

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.core.common.AppResult

/** Periodo del reporte (los rangos los calcula [ReportPeriods]). */
enum class ReportPeriod { THIS_MONTH, LAST_MONTH, LAST_3_MONTHS, THIS_YEAR, ALL_TIME }

/** Qué se quiere ver: un periodo y toda la flota (vehicleId == null) o un solo carro. */
data class ReportFilters(
    val period: ReportPeriod = ReportPeriod.THIS_MONTH,
    val vehicleId: String? = null,
)

enum class ReportFormat { PDF, EXCEL }

/** Reporte de la flota tal como lo calculó la API. */
data class FleetReport(
    val companyName: String,
    /** "yyyy-MM-ddTHH:mm:ss" o null (todo el historial). */
    val from: String?,
    val to: String?,
    val generatedAt: String,
    val summary: ReportSummary,
    val vehicles: List<VehicleStats>,
)

data class ReportSummary(
    val vehicles: Int,
    val activeVehicles: Int,
    val trips: Int,
    val km: Double,
    val gpsKm: Double,
    val fuelLoads: Int,
    val liters: Double,
    val spent: Double,
    val avgPricePerLiter: Double?,
    val kmPerLiter: Double?,
    val costPerKm: Double?,
)

data class VehicleStats(
    val id: String,
    val plates: String,
    val brand: String,
    val model: String,
    val year: Int?,
    val status: VehicleStatus,
    val retired: Boolean,
    val trips: Int,
    val km: Double,
    val fuelLoads: Int,
    val liters: Double,
    val spent: Double,
    val kmPerLiter: Double?,
    val costPerKm: Double?,
    val lastTripAt: String?,
)

enum class VehicleStatus {
    AVAILABLE, BUSY, MAINTENANCE, UNKNOWN;

    companion object {
        fun fromApi(value: String?) = when (value?.lowercase()) {
            "disponible" -> AVAILABLE
            "ocupado" -> BUSY
            "enmantenimiento" -> MAINTENANCE
            else -> UNKNOWN
        }
    }
}

/** Opción del selector de carro ("ABC-123 · Nissan Versa"). */
data class ReportVehicleOption(val id: String, val label: String)

/** Último reporte guardado en el teléfono y con qué filtros se pidió. */
data class SavedReport(val filters: ReportFilters, val report: FleetReport)

/** Archivo descargado de la API listo para compartir. */
class ReportFile(val name: String, val mimeType: String, val bytes: ByteArray)

/**
 * Reportes offline-first: la UI lee el último reporte guardado; [refresh] lo vuelve a pedir.
 * Los archivos (PDF / Excel) siempre se piden a la API: no se guardan en el teléfono.
 */
interface ReportsRepository {
    fun observeReport(): Flow<SavedReport?>

    /** Carros para el selector (salen del último reporte de toda la flota). */
    fun observeVehicles(): Flow<List<ReportVehicleOption>>

    suspend fun refresh(filters: ReportFilters): AppResult<Unit>
    suspend fun download(filters: ReportFilters, format: ReportFormat): AppResult<ReportFile>
}

class ObserveReportUseCase(private val repository: ReportsRepository) {
    operator fun invoke(): Flow<SavedReport?> = repository.observeReport()
}

class ObserveReportVehiclesUseCase(private val repository: ReportsRepository) {
    operator fun invoke(): Flow<List<ReportVehicleOption>> = repository.observeVehicles()
}

class RefreshReportUseCase(private val repository: ReportsRepository) {
    suspend operator fun invoke(filters: ReportFilters): AppResult<Unit> = repository.refresh(filters)
}

class DownloadReportUseCase(private val repository: ReportsRepository) {
    suspend operator fun invoke(filters: ReportFilters, format: ReportFormat): AppResult<ReportFile> =
        repository.download(filters, format)
}
