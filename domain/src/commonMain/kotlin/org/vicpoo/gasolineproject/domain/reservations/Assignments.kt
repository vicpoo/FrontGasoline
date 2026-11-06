package org.vicpoo.gasolineproject.domain.reservations

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Estado del carro tal como lo manda la API. */
enum class VehicleState {
    AVAILABLE, OCCUPIED, MAINTENANCE, UNKNOWN;

    companion object {
        fun fromApi(value: String?): VehicleState = when (value?.lowercase()) {
            "disponible" -> AVAILABLE
            "ocupado" -> OCCUPIED
            "enmantenimiento" -> MAINTENANCE
            else -> UNKNOWN
        }
    }
}

/** Tipo de vehículo (para el ícono de la tarjeta, como en Vehículos). */
enum class AssignmentVehicleType(val apiValue: String) {
    CAR("carro"),
    MOTORCYCLE("moto"),
    PICKUP("camioneta"),
    TRUCK("camion");

    companion object {
        /** Lo desconocido o vacío (API sin el campo) se muestra como carro. */
        fun fromApi(value: String?): AssignmentVehicleType {
            val v = value?.trim()?.lowercase()
            return entries.firstOrNull { it.apiValue == v } ?: CAR
        }
    }
}

data class AssignmentVehicle(
    val id: String,
    /** "Nissan Versa 2022" */
    val title: String,
    val plates: String,
    val color: String?,
    val photoUrl: String?,
    val state: VehicleState,
    val type: AssignmentVehicleType = AssignmentVehicleType.CAR,
)

data class AssignmentEmployee(
    val id: String,
    val name: String,
    val photoUrl: String?,
)

/** Una asignación = una reserva aprobada vigente. Con viaje en curso no se puede desvincular. */
data class Assignment(
    val reservationId: String,
    val since: String?,
    val hasActiveTrip: Boolean,
)

/** Segmento "Carros": un carro vinculado y quién lo tiene. */
data class VehicleAssignment(
    val vehicle: AssignmentVehicle,
    val holder: AssignmentEmployee?,
    val assignment: Assignment?,
)

/** Un carro dentro de la tarjeta del empleado. */
data class EmployeeVehicle(
    val vehicle: AssignmentVehicle,
    val assignment: Assignment,
)

/** Segmento "Empleados": un empleado y los carros que tiene vinculados. */
data class EmployeeAssignments(
    val employee: AssignmentEmployee,
    val vehicles: List<EmployeeVehicle>,
)

/**
 * Qué abre el detalle: una asignación (desde "Carros") o un empleado con todos sus carros
 * (desde "Empleados"). Se guarda como texto ("r:<id>" / "e:<id>") para la navegación.
 */
sealed interface AssignmentKey {
    val value: String

    data class Reservation(val reservationId: String) : AssignmentKey {
        override val value: String get() = "r:$reservationId"
    }

    data class Employee(val employeeId: String) : AssignmentKey {
        override val value: String get() = "e:$employeeId"
    }

    companion object {
        fun parse(value: String): AssignmentKey? = when {
            value.startsWith("r:") -> Reservation(value.removePrefix("r:"))
            value.startsWith("e:") -> Employee(value.removePrefix("e:"))
            else -> null
        }
    }
}

/** Lo que muestra el detalle: el empleado y su(s) carro(s), cada uno con "Desvincular". */
data class AssignmentDetail(
    val employee: AssignmentEmployee,
    val vehicles: List<EmployeeVehicle>,
)

data class AssignmentsOverview(
    val vehicles: List<VehicleAssignment> = emptyList(),
    val employees: List<EmployeeAssignments> = emptyList(),
) {
    /** El detalle de [key], o null si ya no existe (ej. se desvinculó desde otro celular). */
    fun detail(key: AssignmentKey): AssignmentDetail? = when (key) {
        is AssignmentKey.Reservation -> {
            val car = vehicles.firstOrNull { it.assignment?.reservationId == key.reservationId }
            val holder = car?.holder
            val assignment = car?.assignment
            if (car != null && holder != null && assignment != null) {
                AssignmentDetail(holder, listOf(EmployeeVehicle(car.vehicle, assignment)))
            } else {
                employees.firstNotNullOfOrNull { e ->
                    e.vehicles.firstOrNull { it.assignment.reservationId == key.reservationId }
                        ?.let { AssignmentDetail(e.employee, listOf(it)) }
                }
            }
        }
        is AssignmentKey.Employee -> employees
            .firstOrNull { it.employee.id == key.employeeId && it.vehicles.isNotEmpty() }
            ?.let { AssignmentDetail(it.employee, it.vehicles) }
    }

    /** La vista solo muestra lo vinculado: carros que alguien tiene y empleados con algún carro. */
    fun linkedOnly(): AssignmentsOverview = AssignmentsOverview(
        vehicles = vehicles.filter { it.holder != null && it.assignment != null },
        employees = employees.filter { it.vehicles.isNotEmpty() },
    )

    /**
     * Búsqueda de la vista: por marca, modelo, placas o nombre del empleado (sin importar mayúsculas).
     * En "Carros" también encuentra el carro por el nombre de quien lo tiene, y en "Empleados"
     * al empleado por las placas de sus carros.
     */
    fun filter(query: String): AssignmentsOverview {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return this
        fun AssignmentVehicle.matches() = title.lowercase().contains(q) || plates.lowercase().contains(q)
        return AssignmentsOverview(
            vehicles = vehicles.filter { it.vehicle.matches() || it.holder?.name?.lowercase()?.contains(q) == true },
            employees = employees.filter { e -> e.employee.name.lowercase().contains(q) || e.vehicles.any { it.vehicle.matches() } },
        )
    }
}

/** Fuente de la vista Asignaciones. Sin caché: desvincular requiere red y datos al momento. */
interface AssignmentsRepository {
    /** Lo último que se cargó de la API (null = todavía nada); lo comparten la lista y el detalle. */
    fun observe(): Flow<AssignmentsOverview?>

    suspend fun load(): AppResult<AssignmentsOverview>
    suspend fun unlink(reservationId: String, reason: String): AppResult<Unit>
}

class GetAssignmentsUseCase(private val repository: AssignmentsRepository) {
    suspend operator fun invoke(): AppResult<AssignmentsOverview> = repository.load()
}

/** Detalle de una asignación o de un empleado, sobre lo último que se cargó (null si ya no existe). */
class ObserveAssignmentDetailUseCase(private val repository: AssignmentsRepository) {
    operator fun invoke(key: AssignmentKey): Flow<AssignmentDetail?> =
        repository.observe().map { overview -> overview?.detail(key) }
}

/** Desvincular: el motivo es obligatorio (le llega al empleado en la notificación). */
class UnlinkAssignmentUseCase(private val repository: AssignmentsRepository) {
    suspend operator fun invoke(reservationId: String, reason: String): AppResult<Unit> {
        val clean = reason.trim()
        if (clean.isEmpty()) return AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED))
        if (clean.length > MAX_REASON) return AppResult.Failure(AppError.Validation(ValidationReason.OUT_OF_RANGE))
        return repository.unlink(reservationId, clean)
    }

    companion object {
        const val MAX_REASON = 255
    }
}
