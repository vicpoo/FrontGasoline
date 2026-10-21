package org.vicpoo.gasolineproject.feature.reservations.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.network.ApiRoutes

/** CreateSolicitudReservaRequest de APIGasoline (POST /api/solicitudes-reserva). El motivo es obligatorio. */
@Serializable
data class CreateReservationRequestDto(
    val empresaId: String,
    val carroId: String,
    val empleadoSolicitanteId: String,
    val motivo: String,
)

/** EmpleadoResumenDto: nombre y foto (tarjeta "Solicitud de ..."). */
@Serializable
data class ReservationEmployeeDto(
    val empleadoId: String,
    val nombreCompleto: String? = null,
    val fotoPerfilUrl: String? = null,
)

/** CarroResumenDto que viene dentro de cada solicitud. */
@Serializable
data class ReservationCarDto(
    val carroId: String,
    val marca: String,
    val modelo: String,
    val anio: Int? = null,
    val color: String? = null,
    val placas: String,
    val fotoVehiculoUrl: String? = null,
)

/** SolicitudReservaDto (GET /api/solicitudes-reserva): ya trae solicitante y carro. */
@Serializable
data class ReservationRequestDto(
    val solicitudReservaId: String,
    val carroId: String,
    val empleadoSolicitanteId: String? = null,
    val motivoSolicitud: String? = null,
    /** "pendiente" | "aprobada" | "rechazada" | "cancelada" | "finalizada" */
    val estado: String,
    val fechaSolicitud: String? = null,
    val fechaResolucion: String? = null,
    val motivoResolucion: String? = null,
    val solicitante: ReservationEmployeeDto? = null,
    val carro: ReservationCarDto? = null,
)

/** SolicitanteDetalleDto: nombre, foto y licencia (para que el confirmador la revise). */
@Serializable
data class ReservationRequesterDetailDto(
    val empleadoId: String,
    val nombreCompleto: String? = null,
    val fotoPerfilUrl: String? = null,
    val tieneLicencia: Boolean = false,
    val fotoLicenciaUrl: String? = null,
    val fechaCargaLicencia: String? = null,
)

/** CarroDetalleDto del detalle de la solicitud. */
@Serializable
data class ReservationCarDetailDto(
    val carroId: String,
    val marca: String,
    val modelo: String,
    val anio: Int? = null,
    val color: String? = null,
    val placas: String,
    val kilometrajeActual: Double? = null,
    val estado: String? = null,
    val dadoDeBaja: Boolean = false,
    val fotoVehiculoUrl: String? = null,
)

/** SolicitudReservaDetalleDto (GET /api/solicitudes-reserva/{id}). */
@Serializable
data class ReservationDetailDto(
    val solicitudReservaId: String,
    val carroId: String,
    val empleadoSolicitanteId: String? = null,
    val motivoSolicitud: String? = null,
    val fechaSolicitud: String? = null,
    val estado: String,
    val solicitante: ReservationRequesterDetailDto? = null,
    val carro: ReservationCarDetailDto? = null,
    val confirmador: ReservationEmployeeDto? = null,
    val fechaResolucion: String? = null,
    val motivoResolucion: String? = null,
)

/** AprobarSolicitudReservaRequest (POST /api/solicitudes-reserva/{id}/aprobar). */
@Serializable
data class ApproveReservationRequestDto(
    val empleadoConfirmadorId: String,
    val motivo: String? = null,
)

/** RechazarSolicitudReservaRequest (POST /api/solicitudes-reserva/{id}/rechazar). El motivo es obligatorio. */
@Serializable
data class RejectReservationRequestDto(
    val empleadoConfirmadorId: String,
    val motivo: String,
)

/** NotificacionDto (solo lo que usa la campana). */
@Serializable
data class ReservationNotificationDto(
    val notificacionId: String,
    /** "resultadoSolicitud" = me aceptaron o rechazaron una solicitud. */
    val tipo: String? = null,
    val leida: Boolean = false,
)

/** Fuente remota de solicitudes de reserva. */
interface ReservationApi {
    suspend fun create(request: CreateReservationRequestDto)

    /** GET /api/solicitudes-reserva?empleadoSolicitanteId= (la API solo regresa las propias sin solicitud.ver_todas). */
    suspend fun getByEmployee(employeeId: String): List<ReservationRequestDto>

    /** GET /api/solicitudes-reserva (con solicitud.ver_todas: todas las de la empresa del token). */
    suspend fun getCompany(): List<ReservationRequestDto>

    suspend fun getDetail(reservationId: String): ReservationDetailDto

    /** Solo si la API no mandó el carro dentro de la solicitud. */
    suspend fun getVehicle(vehicleId: String): ReservationCarDto

    suspend fun approve(reservationId: String, request: ApproveReservationRequestDto)
    suspend fun reject(reservationId: String, request: RejectReservationRequestDto)

    /** Respuestas a mis solicitudes que no he visto (GET /api/notificaciones?soloNoLeidas=true&tipo=resultadoSolicitud). */
    suspend fun getUnreadReservationResults(employeeId: String): List<ReservationNotificationDto>
    suspend fun markNotificationRead(notificationId: String)
}

class KtorReservationApi(private val client: HttpClient) : ReservationApi {
    override suspend fun create(request: CreateReservationRequestDto) {
        client.post(ApiRoutes.RESERVATION_REQUESTS) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun getByEmployee(employeeId: String): List<ReservationRequestDto> =
        client.get(ApiRoutes.RESERVATION_REQUESTS) { parameter("empleadoSolicitanteId", employeeId) }.body()

    override suspend fun getCompany(): List<ReservationRequestDto> =
        client.get(ApiRoutes.RESERVATION_REQUESTS).body()

    override suspend fun getDetail(reservationId: String): ReservationDetailDto =
        client.get(ApiRoutes.reservation(reservationId)).body()

    override suspend fun getVehicle(vehicleId: String): ReservationCarDto =
        client.get(ApiRoutes.vehicle(vehicleId)).body()

    override suspend fun approve(reservationId: String, request: ApproveReservationRequestDto) {
        client.post(ApiRoutes.reservationApprove(reservationId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun reject(reservationId: String, request: RejectReservationRequestDto) {
        client.post(ApiRoutes.reservationReject(reservationId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun getUnreadReservationResults(employeeId: String): List<ReservationNotificationDto> =
        client.get(ApiRoutes.NOTIFICATIONS) {
            parameter("empleadoDestinatarioId", employeeId)
            parameter("soloNoLeidas", true)
            parameter("tipo", "resultadoSolicitud")
        }.body()

    override suspend fun markNotificationRead(notificationId: String) {
        client.post(ApiRoutes.notificationRead(notificationId))
    }
}
