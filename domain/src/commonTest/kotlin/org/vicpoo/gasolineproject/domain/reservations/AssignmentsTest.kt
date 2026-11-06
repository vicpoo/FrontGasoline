package org.vicpoo.gasolineproject.domain.reservations

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Solo lo vinculado, búsqueda de la vista Asignaciones y validación del motivo al desvincular. */
class AssignmentsTest {

    private fun car(id: String, title: String, plates: String) =
        AssignmentVehicle(id, title, plates, color = null, photoUrl = null, state = VehicleState.OCCUPIED)

    private val versa = car("1", "Nissan Versa 2022", "ABC-123")
    private val aveo = car("2", "Chevrolet Aveo 2020", "XYZ-987")
    private val ana = AssignmentEmployee("e1", "Ana López", null)
    private val beto = AssignmentEmployee("e2", "Beto Ruiz", null)

    private val overview = AssignmentsOverview(
        vehicles = listOf(
            VehicleAssignment(versa, holder = ana, assignment = Assignment("r1", null, hasActiveTrip = false)),
            VehicleAssignment(aveo, holder = null, assignment = null),
        ),
        employees = listOf(
            EmployeeAssignments(ana, listOf(EmployeeVehicle(versa, Assignment("r1", null, false)))),
            EmployeeAssignments(beto, emptyList()),
        ),
    )

    @Test
    fun emptyQuery_returnsEverything() {
        assertEquals(overview, overview.filter("  "))
    }

    @Test
    fun byPlates_findsTheCarAndItsHolder() {
        val result = overview.filter("abc")
        assertEquals(listOf("1"), result.vehicles.map { it.vehicle.id })
        assertEquals(listOf("e1"), result.employees.map { it.employee.id })
    }

    @Test
    fun byEmployeeName_findsTheEmployeeAndTheCarTheyHave() {
        val result = overview.filter("ANA")
        assertEquals(listOf("1"), result.vehicles.map { it.vehicle.id })
        assertEquals(listOf("e1"), result.employees.map { it.employee.id })
    }

    @Test
    fun linkedOnly_dropsCarsWithoutEmployeeAndEmployeesWithoutCar() {
        val result = overview.linkedOnly()
        assertEquals(listOf("1"), result.vehicles.map { it.vehicle.id })
        assertEquals(listOf("e1"), result.employees.map { it.employee.id })
    }

    // ---------------- Detalle ----------------

    @Test
    fun detail_byReservation_isTheCarAndItsHolder() {
        val detail = assertNotNull(overview.detail(AssignmentKey.Reservation("r1")))
        assertEquals("e1", detail.employee.id)
        assertEquals(listOf("1"), detail.vehicles.map { it.vehicle.id })
    }

    @Test
    fun detail_byEmployee_hasAllTheirCars() {
        val detail = assertNotNull(overview.detail(AssignmentKey.Employee("e1")))
        assertEquals(listOf("r1"), detail.vehicles.map { it.assignment.reservationId })
    }

    @Test
    fun detail_alreadyUnlinked_isNull() {
        assertNull(overview.detail(AssignmentKey.Reservation("r-que-ya-no-existe")))
        // Un empleado sin carros no tiene detalle (ya no tiene nada que desvincular)
        assertNull(overview.detail(AssignmentKey.Employee("e2")))
    }

    @Test
    fun key_roundTrip() {
        assertEquals(AssignmentKey.Reservation("r1"), AssignmentKey.parse(AssignmentKey.Reservation("r1").value))
        assertEquals(AssignmentKey.Employee("e1"), AssignmentKey.parse(AssignmentKey.Employee("e1").value))
        assertNull(AssignmentKey.parse("x:1"))
    }

    @Test
    fun vehicleType_fromApi_unknownIsCar() {
        assertEquals(AssignmentVehicleType.PICKUP, AssignmentVehicleType.fromApi("Camioneta"))
        assertEquals(AssignmentVehicleType.CAR, AssignmentVehicleType.fromApi(null))
        assertEquals(AssignmentVehicleType.CAR, AssignmentVehicleType.fromApi("avión"))
    }

    private class FakeRepository : AssignmentsRepository {
        var unlinked: Pair<String, String>? = null
        override fun observe(): Flow<AssignmentsOverview?> = flowOf(null)
        override suspend fun load(): AppResult<AssignmentsOverview> = AppResult.Success(AssignmentsOverview())
        override suspend fun unlink(reservationId: String, reason: String): AppResult<Unit> {
            unlinked = reservationId to reason
            return AppResult.Success(Unit)
        }
    }

    @Test
    fun unlink_withoutReason_isRejectedWithoutCallingTheApi() = runTest {
        val repository = FakeRepository()
        val result = UnlinkAssignmentUseCase(repository)("r1", "   ")
        assertEquals(AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED)), result)
        assertNull(repository.unlinked)
    }

    @Test
    fun unlink_trimsTheReason() = runTest {
        val repository = FakeRepository()
        UnlinkAssignmentUseCase(repository)("r1", "  Otra ruta  ")
        assertEquals("r1" to "Otra ruta", repository.unlinked)
    }
}
