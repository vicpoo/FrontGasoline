package org.vicpoo.gasolineproject.feature.reservations.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.onSuccess
import org.vicpoo.gasolineproject.core.common.map
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.feature.reservations.data.remote.AssignmentsApi
import org.vicpoo.gasolineproject.feature.reservations.data.remote.AssignmentsDto
import org.vicpoo.gasolineproject.feature.reservations.data.remote.ReservationCarDto
import org.vicpoo.gasolineproject.feature.reservations.data.remote.ReservationEmployeeDto
import org.vicpoo.gasolineproject.feature.reservations.data.remote.UnlinkAssignmentRequestDto
import org.vicpoo.gasolineproject.feature.reservations.domain.Assignment
import org.vicpoo.gasolineproject.feature.reservations.domain.AssignmentEmployee
import org.vicpoo.gasolineproject.feature.reservations.domain.AssignmentVehicle
import org.vicpoo.gasolineproject.feature.reservations.domain.AssignmentVehicleType
import org.vicpoo.gasolineproject.feature.reservations.domain.AssignmentsOverview
import org.vicpoo.gasolineproject.feature.reservations.domain.AssignmentsRepository
import org.vicpoo.gasolineproject.feature.reservations.domain.EmployeeAssignments
import org.vicpoo.gasolineproject.feature.reservations.domain.EmployeeVehicle
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRepository
import org.vicpoo.gasolineproject.feature.reservations.domain.VehicleAssignment
import org.vicpoo.gasolineproject.feature.reservations.domain.VehicleState

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
