package org.vicpoo.gasolineproject.feature.triprecording.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.network.ApiRoutes

// ======================= DTOs (contratos de APIGasoline) =======================

/** SolicitudReservaDto (solo los campos que usa la app). */
@Serializable
data class SolicitudReservaDto(
    val solicitudReservaId: String,
    val empresaId: String,
    val carroId: String,
    val empleadoSolicitanteId: String,
    /** "pendiente" | "aprobada" | "rechazada" | "cancelada" | "finalizada" */
    val estado: String? = null,
    val fechaSolicitud: String? = null,
)

/** CarroDto (GET /api/carros/{id}). */
@Serializable
data class AssignedCarroDto(
    val carroId: String,
    val marca: String,
    val modelo: String,
    val color: String? = null,
    val placas: String,
    val fotoVehiculoUrl: String? = null,
    val capacidadTanque: Double? = null,
    val kilometrajeActual: Double = 0.0,
    val dadoDeBaja: Boolean = false,
)

/** CentroCostoDto (GET /api/centros-costo?empresaId=). */
@Serializable
data class CentroCostoDto(
    val centroCostoId: String,
    val empresaId: String,
    val nombre: String,
    val clave: String,
    val activo: Boolean = true,
)

/** IniciarViajeRequest (POST /api/viajes). Fechas en hora local sin zona. */
@Serializable
data class IniciarViajeRequestDto(
    val empresaId: String,
    val carroId: String,
    val solicitudReservaId: String?,
    val empleadoOperadorId: String,
    val fechaInicio: String,
    val ubicacionInicioLat: Double?,
    val ubicacionInicioLng: Double?,
    val kilometrajeInicial: Double,
    /** Centro de costos elegido en "Iniciar viaje" (null si la empresa no tiene). */
    val centroCostoId: String? = null,
)

/** SincronizarViajeRequest (POST /api/viajes/sincronizar): viaje completo hecho sin red. */
@Serializable
data class SincronizarViajeRequestDto(
    val empresaId: String,
    val carroId: String,
    val solicitudReservaId: String?,
    val empleadoOperadorId: String,
    val fechaInicio: String,
    val ubicacionInicioLat: Double?,
    val ubicacionInicioLng: Double?,
    val kilometrajeInicial: Double,
    /** "finalizado" */
    val estado: String,
    val fechaFin: String?,
    val ubicacionFinLat: Double?,
    val ubicacionFinLng: Double?,
    val kilometrajeFinal: Double?,
    val centroCostoId: String? = null,
    /** Pausas hechas sin conexión (fechaFin null = sigue abierta). */
    val pausas: List<PausaSincronizadaDto>? = null,
)

/** PausaSincronizadaRequest (dentro de SincronizarViajeRequest). */
@Serializable
data class PausaSincronizadaDto(
    val fechaInicio: String,
    val fechaFin: String?,
    val motivo: String? = null,
    val ubicacionLat: Double? = null,
    val ubicacionLng: Double? = null,
)

/** PausarViajeRequest (POST /api/viajes/{id}/pausar). */
@Serializable
data class PausarViajeRequestDto(
    val fecha: String,
    val motivo: String? = null,
    val ubicacionLat: Double? = null,
    val ubicacionLng: Double? = null,
)

/** ReanudarViajeRequest (POST /api/viajes/{id}/reanudar). */
@Serializable
data class ReanudarViajeRequestDto(val fecha: String)

/** FinalizarViajeRequest (POST /api/viajes/{id}/finalizar). */
@Serializable
data class FinalizarViajeRequestDto(
    val fechaFin: String,
    val ubicacionFinLat: Double?,
    val ubicacionFinLng: Double?,
    val kilometrajeFinal: Double,
)

@Serializable
data class ViajeCreadoDto(
    val viajeId: String,
    val estado: String? = null,
)

/** PuntoRutaRequest (dentro de RegistrarLotePuntosRutaRequest, máx. 1000 por lote). */
@Serializable
data class PuntoRutaRequestDto(
    val latitud: Double,
    val longitud: Double,
    val timestampCaptura: String,
    val precisionMetros: Double? = null,
    val altitud: Double? = null,
    val velocidad: Double? = null,
)

@Serializable
data class LotePuntosRutaRequestDto(val puntos: List<PuntoRutaRequestDto>)

/** LotePuntosRutaResultadoDto: el mismo lote se puede reenviar sin duplicar puntos. */
@Serializable
data class LotePuntosRutaResultadoDto(
    val viajeId: String,
    val recibidos: Int = 0,
    val insertados: Int = 0,
    val duplicados: Int = 0,
)

/** CargaCombustibleDto (respuesta de POST / PUT /api/cargas-combustible). */
@Serializable
data class CargaCreadaDto(
    val cargaCombustibleId: String,
    val viajeId: String,
    val litros: Double = 0.0,
    val monto: Double? = null,
    val fotoComprobanteUrl: String? = null,
    val fechaCarga: String? = null,
)

/** Campos del multipart de CreateCargaCombustibleRequest / UpdateCargaCombustibleRequest. */
data class CargaFormData(
    val viajeId: String?,
    val litros: Double,
    val monto: Double?,
    val fechaCarga: String,
    val ubicacionLat: Double?,
    val ubicacionLng: Double?,
    /** Solo en PUT (obligatorio para la auditoría). */
    val motivo: String?,
    val centroCostoId: String?,
    val receipt: ReceiptUpload?,
)

class ReceiptUpload(val bytes: ByteArray, val fileName: String, val mimeType: String)

// ======================= API =======================

/** Endpoints que usa el registro de viajes (Interface Segregation: solo lo necesario). */
interface TripRecordingApi {
    suspend fun getApprovedReservations(employeeId: String): List<SolicitudReservaDto>
    suspend fun getVehicle(vehicleId: String): AssignedCarroDto
    suspend fun getCostCenters(companyId: String): List<CentroCostoDto>

    suspend fun startTrip(request: IniciarViajeRequestDto): ViajeCreadoDto
    suspend fun syncFinishedTrip(request: SincronizarViajeRequestDto): ViajeCreadoDto
    suspend fun finishTrip(tripId: String, request: FinalizarViajeRequestDto): ViajeCreadoDto
    suspend fun sendRoutePoints(tripId: String, points: List<PuntoRutaRequestDto>): LotePuntosRutaResultadoDto

    suspend fun pauseTrip(tripId: String, request: PausarViajeRequestDto): ViajeCreadoDto
    suspend fun resumeTrip(tripId: String, request: ReanudarViajeRequestDto): ViajeCreadoDto

    /** PUT /api/viajes/{id}/foto-tablero/{momento}: multipart con el campo "archivo". */
    suspend fun uploadDashboardPhoto(tripId: String, momento: String, photo: ReceiptUpload): ViajeCreadoDto

    suspend fun createFuelLoad(form: CargaFormData): CargaCreadaDto
    suspend fun updateFuelLoad(fuelLoadId: String, form: CargaFormData): CargaCreadaDto
    /** El motivo es obligatorio: la API lo guarda en la auditoría. */
    suspend fun deleteFuelLoad(fuelLoadId: String, motivo: String)
}

class KtorTripRecordingApi(private val client: HttpClient) : TripRecordingApi {

    override suspend fun getApprovedReservations(employeeId: String): List<SolicitudReservaDto> =
        client.get(ApiRoutes.RESERVATION_REQUESTS) {
            parameter("empleadoSolicitanteId", employeeId)
            parameter("estado", "aprobada")
        }.body()

    override suspend fun getVehicle(vehicleId: String): AssignedCarroDto =
        client.get(ApiRoutes.vehicle(vehicleId)).body()

    override suspend fun getCostCenters(companyId: String): List<CentroCostoDto> =
        client.get(ApiRoutes.COST_CENTERS) { parameter("empresaId", companyId) }.body()

    override suspend fun startTrip(request: IniciarViajeRequestDto): ViajeCreadoDto =
        client.post(ApiRoutes.TRIPS) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun syncFinishedTrip(request: SincronizarViajeRequestDto): ViajeCreadoDto =
        client.post(ApiRoutes.TRIPS_SYNC) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun finishTrip(tripId: String, request: FinalizarViajeRequestDto): ViajeCreadoDto =
        client.post(ApiRoutes.tripFinish(tripId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun sendRoutePoints(tripId: String, points: List<PuntoRutaRequestDto>): LotePuntosRutaResultadoDto =
        client.post(ApiRoutes.tripRoutePoints(tripId)) {
            contentType(ContentType.Application.Json)
            setBody(LotePuntosRutaRequestDto(points))
        }.body()

    override suspend fun pauseTrip(tripId: String, request: PausarViajeRequestDto): ViajeCreadoDto =
        client.post(ApiRoutes.tripPause(tripId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun resumeTrip(tripId: String, request: ReanudarViajeRequestDto): ViajeCreadoDto =
        client.post(ApiRoutes.tripResume(tripId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun uploadDashboardPhoto(tripId: String, momento: String, photo: ReceiptUpload): ViajeCreadoDto =
        client.put(ApiRoutes.tripDashboardPhoto(tripId, momento)) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "archivo",
                            value = photo.bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, photo.mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"${photo.fileName}\"")
                            },
                        )
                    },
                ),
            )
        }.body()

    /** POST /api/cargas-combustible: multipart/form-data con el archivo "fotoComprobante". */
    override suspend fun createFuelLoad(form: CargaFormData): CargaCreadaDto =
        client.post(ApiRoutes.FUEL_LOADS) { setBody(form.toMultipart()) }.body()

    /** PUT /api/cargas-combustible/{id}: "fotoComprobante" opcional, "motivo" obligatorio. */
    override suspend fun updateFuelLoad(fuelLoadId: String, form: CargaFormData): CargaCreadaDto =
        client.put(ApiRoutes.fuelLoad(fuelLoadId)) { setBody(form.toMultipart()) }.body()

    /** DELETE /api/cargas-combustible/{id}?motivo=... */
    override suspend fun deleteFuelLoad(fuelLoadId: String, motivo: String) {
        client.delete(ApiRoutes.fuelLoad(fuelLoadId)) { parameter("motivo", motivo) }
    }
}

/** Arma el formulario con los nombres de campo de los contratos [FromForm] (camelCase). */
private fun CargaFormData.toMultipart(): MultiPartFormDataContent = MultiPartFormDataContent(
    formData {
        viajeId?.let { append("viajeId", it) }
        append("litros", litros.plain(2))
        monto?.let { append("monto", it.plain(2)) }
        append("fechaCarga", fechaCarga)
        if (ubicacionLat != null && ubicacionLng != null) {
            append("ubicacionLat", ubicacionLat.plain(7))
            append("ubicacionLng", ubicacionLng.plain(7))
        }
        motivo?.let { append("motivo", it) }
        centroCostoId?.let { append("centroCostoId", it) }
        receipt?.let { file ->
            append(
                key = "fotoComprobante",
                value = file.bytes,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, file.mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
                },
            )
        }
    },
)

/**
 * Número sin notación científica y con punto decimal ("10000000.00", "-99.1332000").
 * Double.toString() puede dar "1.0E7", que el model binding de ASP.NET no siempre acepta.
 */
internal fun Double.plain(decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = kotlin.math.round(this * factor).toLong()
    val negative = scaled < 0
    val absolute = if (negative) -scaled else scaled
    val integerPart = absolute / factor
    val fraction = (absolute % factor).toString().padStart(decimals, '0')
    val sign = if (negative) "-" else ""
    return if (decimals == 0) "$sign$integerPart" else "$sign$integerPart.$fraction"
}
