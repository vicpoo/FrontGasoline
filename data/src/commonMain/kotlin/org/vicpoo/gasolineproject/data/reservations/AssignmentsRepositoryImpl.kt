package org.vicpoo.gasolineproject.data.reservations

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.onSuccess
import org.vicpoo.gasolineproject.domain.common.map
import org.vicpoo.gasolineproject.data.network.safeApiCall
import org.vicpoo.gasolineproject.data.reservations.remote.AssignmentsApi
import org.vicpoo.gasolineproject.data.reservations.remote.AssignmentsDto
import org.vicpoo.gasolineproject.data.reservations.remote.ReservationCarDto
import org.vicpoo.gasolineproject.data.reservations.remote.ReservationEmployeeDto
import org.vicpoo.gasolineproject.data.reservations.remote.UnlinkAssignmentRequestDto
import org.vicpoo.gasolineproject.domain.reservations.Assignment
import org.vicpoo.gasolineproject.domain.reservations.AssignmentEmployee
import org.vicpoo.gasolineproject.domain.reservations.AssignmentVehicle
import org.vicpoo.gasolineproject.domain.reservations.AssignmentVehicleType
import org.vicpoo.gasolineproject.domain.reservations.AssignmentsOverview
import org.vicpoo.gasolineproject.domain.reservations.AssignmentsRepository
import org.vicpoo.gasolineproject.domain.reservations.EmployeeAssignments
import org.vicpoo.gasolineproject.domain.reservations.EmployeeVehicle
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository
import org.vicpoo.gasolineproject.domain.reservations.VehicleAssignment
import org.vicpoo.gasolineproject.domain.reservations.VehicleState

/**
 * Vista Asignaciones, siempre de la API (sin caché en disco: desvincular requiere datos al momento).
 * Lo último que llegó se guarda en memoria para que el detalle abra al instante.
 * Al desvincular se recargan también las solicitudes de la empresa, porque esa reserva pasa a "finalizada".
 */
class AssignmentsRepositoryImpl(
    private val api: AssignmentsApi,
    private val reservations: ReservationRepository,
) : AssignmentsRepository {

    private val last = MutableStateFlow<AssignmentsOverview?>(null)

    override fun observe(): Flow<AssignmentsOverview?> = last.asStateFlow()

    override suspend fun load(): AppResult<AssignmentsOverview> =
        safeApiCall { api.getAll() }
            .map { it.toDomain().linkedOnly() }
            .onSuccess { last.value = it }

    override suspend fun unlink(reservationId: String, reason: String): AppResult<Unit> {
        val result = safeApiCall { api.unlink(reservationId, UnlinkAssignmentRequestDto(motivo = reason)) }
        if (result is AppResult.Success) reservations.refreshCompany()
        return result
    }
}

internal fun AssignmentsDto.toDomain() = AssignmentsOverview(
    vehicles = carros.map { car ->
        VehicleAssignment(
            vehicle = AssignmentVehicle(
                id = car.carroId,
                title = vehicleTitle(car.marca, car.modelo, car.anio),
                plates = car.placas,
                color = car.color,
                photoUrl = car.fotoVehiculoUrl,
                state = VehicleState.fromApi(car.estado),
                type = AssignmentVehicleType.fromApi(car.tipoVehiculo),
            ),
            holder = car.asignacion?.empleado?.toDomain(),
            assignment = car.asignacion?.let { Assignment(it.solicitudReservaId, it.desde, it.tieneViajeEnCurso) },
        )
    },
    employees = empleados.map { employee ->
        EmployeeAssignments(
            employee = AssignmentEmployee(employee.empleadoId, employee.nombreCompleto.orEmpty(), employee.fotoPerfilUrl),
            vehicles = employee.carros.map { item ->
                EmployeeVehicle(
                    vehicle = item.carro.toDomain(VehicleState.fromApi(item.estadoCarro)),
                    assignment = Assignment(item.solicitudReservaId, item.desde, item.tieneViajeEnCurso),
                )
            },
        )
    },
)

private fun ReservationEmployeeDto.toDomain() = AssignmentEmployee(empleadoId, nombreCompleto.orEmpty(), fotoPerfilUrl)

private fun ReservationCarDto.toDomain(state: VehicleState) = AssignmentVehicle(
    id = carroId,
    title = vehicleTitle(marca, modelo, anio),
    plates = placas,
    color = color,
    photoUrl = fotoVehiculoUrl,
    state = state,
    type = AssignmentVehicleType.fromApi(tipoVehiculo),
)

private fun vehicleTitle(brand: String, model: String, year: Int?) =
    listOfNotNull(brand, model, year?.toString()).filter { it.isNotBlank() }.joinToString(" ")
