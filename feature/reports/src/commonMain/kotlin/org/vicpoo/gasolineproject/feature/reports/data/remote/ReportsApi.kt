package org.vicpoo.gasolineproject.feature.reports.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.network.ApiRoutes

/** ReporteFlotaDto de APIGasoline (GET /api/reportes/flota). Guid -> String; decimal -> Double. */
@Serializable
data class ReporteFlotaDto(
    val empresaId: String? = null,
    val empresa: String = "",
    val carroId: String? = null,
    val desde: String? = null,
    val hasta: String? = null,
    val generadoEn: String = "",
    val resumen: ResumenFlotaDto = ResumenFlotaDto(),
    val carros: List<CarroReporteDto> = emptyList(),
)

@Serializable
data class ResumenFlotaDto(
    val carros: Int = 0,
    val carrosConActividad: Int = 0,
    val viajes: Int = 0,
    val kmRecorridos: Double = 0.0,
    val kmGps: Double = 0.0,
    val cargas: Int = 0,
    val litros: Double = 0.0,
    val gasto: Double = 0.0,
    val precioPromedioLitro: Double? = null,
    val rendimientoKmPorLitro: Double? = null,
    val costoPorKm: Double? = null,
)

@Serializable
data class CarroReporteDto(
    val carroId: String,
    val placas: String = "",
    val marca: String = "",
    val modelo: String = "",
    val anio: Int? = null,
    /** "disponible" | "ocupado" | "enMantenimiento" */
    val estado: String? = null,
    val dadoDeBaja: Boolean = false,
    val kilometrajeActual: Double = 0.0,
    val viajes: Int = 0,
    val kmRecorridos: Double = 0.0,
    val kmGps: Double = 0.0,
    val cargas: Int = 0,
    val litros: Double = 0.0,
    val gasto: Double = 0.0,
    val rendimientoKmPorLitro: Double? = null,
    val costoPorKm: Double? = null,
    val ultimoViaje: String? = null,
)

/** Filtros tal como los recibe la API: fechas "yyyy-MM-ddTHH:mm:ss" (null = sin límite). */
data class ReportQuery(
    val vehicleId: String? = null,
    val from: String? = null,
    val to: String? = null,
)

/** Archivo que generó la API (PDF o Excel). */
class DownloadedFile(val name: String?, val mimeType: String?, val bytes: ByteArray)

/** Fuente remota de reportes (ReportesController, privilegio auditoria.ver). */
interface ReportsApi {
    suspend fun getFleetReport(query: ReportQuery): ReporteFlotaDto
    suspend fun downloadPdf(query: ReportQuery): DownloadedFile
    suspend fun downloadExcel(query: ReportQuery): DownloadedFile
}

/**
 * La empresa NO se manda: la API usa la del token (y con plataforma.multiempresa, todas).
 */
class KtorReportsApi(private val client: HttpClient) : ReportsApi {

    override suspend fun getFleetReport(query: ReportQuery): ReporteFlotaDto =
        client.get(ApiRoutes.FLEET_REPORT) { filters(query) }.body()

    override suspend fun downloadPdf(query: ReportQuery): DownloadedFile =
        client.get(ApiRoutes.FLEET_REPORT_PDF) { filters(query) }.toFile()

    override suspend fun downloadExcel(query: ReportQuery): DownloadedFile =
        client.get(ApiRoutes.FLEET_REPORT_EXCEL) { filters(query) }.toFile()

    private fun HttpRequestBuilder.filters(query: ReportQuery) {
        query.vehicleId?.let { parameter("carroId", it) }
        query.from?.let { parameter("desde", it) }
        query.to?.let { parameter("hasta", it) }
    }

    private suspend fun HttpResponse.toFile(): DownloadedFile {
        val name = headers[HttpHeaders.ContentDisposition]?.let { header ->
            runCatching {
                ContentDisposition.parse(header).parameter(ContentDisposition.Parameters.FileName)
            }.getOrNull()
        }
        return DownloadedFile(
            name = name?.trim('"')?.takeIf { it.isNotBlank() },
            mimeType = contentType()?.let { "${it.contentType}/${it.contentSubtype}" },
            bytes = body<ByteArray>(),
        )
    }
}
