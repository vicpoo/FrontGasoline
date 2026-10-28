package org.vicpoo.gasolineproject.feature.reports.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.common.AppClock
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.feature.reports.data.remote.CarroReporteDto
import org.vicpoo.gasolineproject.feature.reports.data.remote.DownloadedFile
import org.vicpoo.gasolineproject.feature.reports.data.remote.ReportQuery
import org.vicpoo.gasolineproject.feature.reports.data.remote.ReporteFlotaDto
import org.vicpoo.gasolineproject.feature.reports.data.remote.ReportsApi
import org.vicpoo.gasolineproject.feature.reports.domain.FleetReport
import org.vicpoo.gasolineproject.feature.reports.domain.ReportFile
import org.vicpoo.gasolineproject.feature.reports.domain.ReportFilters
import org.vicpoo.gasolineproject.feature.reports.domain.ReportFormat
import org.vicpoo.gasolineproject.feature.reports.domain.ReportPeriod
import org.vicpoo.gasolineproject.feature.reports.domain.ReportPeriods
import org.vicpoo.gasolineproject.feature.reports.domain.ReportSummary
import org.vicpoo.gasolineproject.feature.reports.domain.ReportVehicleOption
import org.vicpoo.gasolineproject.feature.reports.domain.ReportsRepository
import org.vicpoo.gasolineproject.feature.reports.domain.SavedReport
import org.vicpoo.gasolineproject.feature.reports.domain.VehicleStats
import org.vicpoo.gasolineproject.feature.reports.domain.VehicleStatus

// ---------- Lo que se guarda en el caché offline ----------

/**
 * Último reporte visto (se guarda tal cual llegó de la API: es de solo lectura) y la lista de
 * carros del último reporte de toda la flota, para el selector aunque no haya red.
 */
@Serializable
data class ReportsSnapshot(
    val period: String? = null,
    val vehicleId: String? = null,
    val report: ReporteFlotaDto? = null,
    val vehicles: List<VehicleOptionEntity> = emptyList(),
)

@Serializable
data class VehicleOptionEntity(val id: String, val label: String)

class ReportsRepositoryImpl(
    private val api: ReportsApi,
    private val cache: JsonFileCache<ReportsSnapshot>,
    private val clock: AppClock,
) : ReportsRepository {

    override fun observeReport(): Flow<SavedReport?> = cache.data.map { snapshot ->
        val report = snapshot.report ?: return@map null
        SavedReport(
            filters = ReportFilters(
                period = ReportPeriod.entries.firstOrNull { it.name == snapshot.period } ?: ReportPeriod.THIS_MONTH,
                vehicleId = snapshot.vehicleId,
            ),
            report = report.toDomain(),
        )
    }

    override fun observeVehicles(): Flow<List<ReportVehicleOption>> =
        cache.data.map { snapshot -> snapshot.vehicles.map { ReportVehicleOption(it.id, it.label) } }

    override suspend fun refresh(filters: ReportFilters): AppResult<Unit> {
        val dto = when (val result = safeApiCall { api.getFleetReport(filters.toQuery()) }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val previous = cache.current()
        // La lista del selector solo se renueva con el reporte de TODA la flota
        val vehicles = if (filters.vehicleId == null) {
            dto.carros.sortedBy { it.placas }.map { VehicleOptionEntity(it.carroId, it.optionLabel()) }
        } else {
            previous.vehicles
        }
        cache.save(
            ReportsSnapshot(
                period = filters.period.name,
                vehicleId = filters.vehicleId,
                report = dto,
                vehicles = vehicles,
            ),
        )
        return AppResult.Success(Unit)
    }

    override suspend fun download(filters: ReportFilters, format: ReportFormat): AppResult<ReportFile> {
        val query = filters.toQuery()
        val result = safeApiCall {
            when (format) {
                ReportFormat.PDF -> api.downloadPdf(query)
                ReportFormat.EXCEL -> api.downloadExcel(query)
            }
        }
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(result.data.toReportFile(format))
        }
    }

    suspend fun clear() = cache.clear()

    /** Fechas del periodo con la hora: el último día se incluye completo. */
    private fun ReportFilters.toQuery(): ReportQuery {
        val range = ReportPeriods.rangeOf(period, clock.nowLocalIso())
        return ReportQuery(
            vehicleId = vehicleId,
            from = range?.let { "${it.from}T00:00:00" },
            to = range?.let { "${it.to}T23:59:59" },
        )
    }
}

private fun DownloadedFile.toReportFile(format: ReportFormat): ReportFile {
    val (defaultName, defaultMime) = when (format) {
        ReportFormat.PDF -> "reporte-flota.pdf" to "application/pdf"
        ReportFormat.EXCEL -> "reporte-flota.xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
    return ReportFile(
        name = name ?: defaultName,
        mimeType = mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" } ?: defaultMime,
        bytes = bytes,
    )
}

private fun CarroReporteDto.optionLabel(): String =
    listOf(marca, modelo).filter { it.isNotBlank() }.joinToString(" ").let { name ->
        if (name.isBlank()) placas else "$placas · $name"
    }

internal fun ReporteFlotaDto.toDomain() = FleetReport(
    companyName = empresa,
    from = desde,
    to = hasta,
    generatedAt = generadoEn,
    summary = with(resumen) {
        ReportSummary(
            vehicles = carros,
            activeVehicles = carrosConActividad,
            trips = viajes,
            km = kmRecorridos,
            gpsKm = kmGps,
            fuelLoads = cargas,
            liters = litros,
            spent = gasto,
            avgPricePerLiter = precioPromedioLitro,
            kmPerLiter = rendimientoKmPorLitro,
            costPerKm = costoPorKm,
        )
    },
    vehicles = carros.map {
        VehicleStats(
            id = it.carroId,
            plates = it.placas,
            brand = it.marca,
            model = it.modelo,
            year = it.anio,
            status = VehicleStatus.fromApi(it.estado),
            retired = it.dadoDeBaja,
            trips = it.viajes,
            km = it.kmRecorridos,
            fuelLoads = it.cargas,
            liters = it.litros,
            spent = it.gasto,
            kmPerLiter = it.rendimientoKmPorLitro,
            costPerKm = it.costoPorKm,
            lastTripAt = it.ultimoViaje,
        )
    },
)
