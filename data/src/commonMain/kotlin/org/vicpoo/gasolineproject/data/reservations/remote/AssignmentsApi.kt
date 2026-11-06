package org.vicpoo.gasolineproject.data.reservations.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.data.network.ApiRoutes

/** AsignacionesDto (GET /api/asignaciones): los dos segmentos de la vista en una sola llamada. */
@Serializable
data class AssignmentsDto(
    val carros: List<CarAssignmentDto> = emptyList(),
    val empleados: List<EmployeeAssignmentDto> = emptyList(),
)

/** CarroAsignacionDto: un carro y, si alguien lo tiene, su asignación. */
@Serializable
data class CarAssignmentDto(
    val carroId: String,
    val marca: String,
    val modelo: String,
    val anio: Int? = null,
    val color: String? = null,
    val placas: String,
    val fotoVehiculoUrl: String? = null,
    /** "disponible" | "ocupado" | "enMantenimiento" */
    val estado: String? = null,
    val asignacion: CarHolderDto? = null,
    /** "carro" | "moto" | "camioneta" | "camion" (para el ícono). */
    val tipoVehiculo: String? = null,
)

/** AsignacionCarroDto: quién tiene el carro y desde cuándo. */
@Serializable
data class CarHolderDto(
    val solicitudReservaId: String,
    val empleado: ReservationEmployeeDto,
    val desde: String? = null,
    val tieneViajeEnCurso: Boolean = false,
)

/** EmpleadoAsignacionDto: un empleado y sus carros (puede no tener ninguno). */
@Serializable
data class EmployeeAssignmentDto(
    val empleadoId: String,
    val nombreCompleto: String? = null,
    val fotoPerfilUrl: String? = null,
    val carros: List<EmployeeCarDto> = emptyList(),
)

/** AsignacionEmpleadoDto: un carro asignado al empleado. */
@Serializable
data class EmployeeCarDto(
    val solicitudReservaId: String,
    val carro: ReservationCarDto,
    val estadoCarro: String? = null,
    val desde: String? = null,
    val tieneViajeEnCurso: Boolean = false,
)

/** DesvincularAsignacionRequest (POST /api/asignaciones/{id}/desvincular). El motivo es obligatorio. */
@Serializable
data class UnlinkAssignmentRequestDto(val motivo: String)

/** Vista Asignaciones (asignacion.gestionar). */
interface AssignmentsApi {
    suspend fun getAll(): AssignmentsDto
    suspend fun unlink(reservationId: String, request: UnlinkAssignmentRequestDto)
}

class KtorAssignmentsApi(private val client: HttpClient) : AssignmentsApi {
    override suspend fun getAll(): AssignmentsDto = client.get(ApiRoutes.ASSIGNMENTS).body()

    override suspend fun unlink(reservationId: String, request: UnlinkAssignmentRequestDto) {
        client.post(ApiRoutes.assignmentUnlink(reservationId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
