package org.vicpoo.gasolineproject.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.Assignment
import org.vicpoo.gasolineproject.domain.reservations.AssignmentEmployee
import org.vicpoo.gasolineproject.domain.reservations.AssignmentVehicle
import org.vicpoo.gasolineproject.domain.reservations.AssignmentsOverview
import org.vicpoo.gasolineproject.domain.reservations.AssignmentsRepository
import org.vicpoo.gasolineproject.domain.reservations.EmployeeAssignments
import org.vicpoo.gasolineproject.domain.reservations.EmployeeVehicle
import org.vicpoo.gasolineproject.domain.reservations.RequestOutcome
import org.vicpoo.gasolineproject.domain.reservations.RequestedVehicle
import org.vicpoo.gasolineproject.domain.reservations.ReservationDetail
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository
import org.vicpoo.gasolineproject.domain.reservations.ReservationRequest
import org.vicpoo.gasolineproject.domain.reservations.ReservationStatus
import org.vicpoo.gasolineproject.domain.reservations.VehicleAssignment
import org.vicpoo.gasolineproject.domain.reservations.VehicleState

/*
 * Fakes de reservas (solicitudes de carro) y asignaciones. Todo en memoria: la prueba cambia
 * los resultados y revisa qué se llamó en [FakeReservationRepository.calls].
 */

/** Solicitud de prueba. */
fun testReservation(
    id: String,
    status: ReservationStatus = ReservationStatus.PENDING,
    requestedAt: String? = "2026-11-20T09:00:00",
    requesterName: String? = "Juan Pérez",
) = ReservationRequest(
    id = id,
    vehicleId = "car-$id",
    vehicleTitle = "Nissan Versa",
    plates = "ABC-123",
    photoUrl = null,
    status = status,
    requestedAt = requestedAt,
    resolvedAt = null,
    resolutionReason = null,
    requestReason = "Visita a cliente",
    requesterId = "emp-1",
    requesterName = requesterName,
)

/** Detalle de prueba de una solicitud. */
fun testReservationDetail(request: ReservationRequest) = ReservationDetail(
    request = request,
    requesterHasLicense = true,
    requesterLicenseUrl = "http://13.222.72.73/fotos/licencia.jpg",
    requesterLicenseUploadedAt = "2026-10-01T10:00:00",
    vehicleYear = 2022,
    vehicleColor = "Blanco",
    vehicleMileageKm = 15_000.0,
    vehicleState = "disponible",
    confirmerName = null,
    confirmerPhotoUrl = null,
)

/**
 * Repositorio de solicitudes en memoria.
 * - Aceptar / rechazar con éxito cambian el estado de la solicitud en la lista de la empresa.
 * - [detailResult] null = se arma el detalle con la solicitud de la empresa (o NotFound).
 */
class FakeReservationRepository(
    initialMine: List<ReservationRequest> = emptyList(),
    initialCompany: List<ReservationRequest> = emptyList(),
) : ReservationRepository {
    val mine = MutableStateFlow(initialMine)
    val company = MutableStateFlow(initialCompany)
    val mineHasMore = MutableStateFlow(false)
    val companyHasMore = MutableStateFlow(false)
    val unseen = MutableStateFlow(0)

    var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
    var loadMoreResult: AppResult<Unit> = AppResult.Success(Unit)
    var detailResult: AppResult<ReservationDetail>? = null
    var approveResult: AppResult<Unit> = AppResult.Success(Unit)
    var rejectResult: AppResult<Unit> = AppResult.Success(Unit)
    var requestResult: AppResult<RequestOutcome> = AppResult.Success(RequestOutcome.SENT)

    /** Lo que se llamó, en orden (ej. "refreshMine", "approve:req-1", "reject:req-1:motivo"). */
    val calls = mutableListOf<String>()

    fun count(call: String): Int = calls.count { it == call }

    override suspend fun requestVehicle(vehicle: RequestedVehicle, reason: String): AppResult<RequestOutcome> {
        calls += "requestVehicle:${vehicle.serverId}:$reason"
        return requestResult
    }

    override fun observeMine(): Flow<List<ReservationRequest>> = mine

    override suspend fun refreshMine(): AppResult<Unit> {
        calls += "refreshMine"
        return refreshResult
    }

    override fun observeMineHasMore(): Flow<Boolean> = mineHasMore

    override suspend fun loadMoreMine(): AppResult<Unit> {
        calls += "loadMoreMine"
        return loadMoreResult
    }

    override fun observeCompany(): Flow<List<ReservationRequest>> = company

    override suspend fun refreshCompany(): AppResult<Unit> {
        calls += "refreshCompany"
        return refreshResult
    }

    override fun observeCompanyHasMore(): Flow<Boolean> = companyHasMore

    override suspend fun loadMoreCompany(): AppResult<Unit> {
        calls += "loadMoreCompany"
        return loadMoreResult
    }

    override fun observeCompanyRequest(reservationId: String): Flow<ReservationRequest?> =
        company.map { list -> list.firstOrNull { it.id == reservationId } }

    override suspend fun getDetail(reservationId: String): AppResult<ReservationDetail> {
        calls += "getDetail:$reservationId"
        detailResult?.let { return it }
        val request = company.value.firstOrNull { it.id == reservationId } ?: return AppResult.Failure(AppError.NotFound)
        return AppResult.Success(testReservationDetail(request))
    }

    override suspend fun approve(reservationId: String): AppResult<Unit> {
        calls += "approve:$reservationId"
        if (approveResult is AppResult.Success) setStatus(reservationId, ReservationStatus.APPROVED)
        return approveResult
    }

    override suspend fun reject(reservationId: String, reason: String): AppResult<Unit> {
        calls += "reject:$reservationId:$reason"
        if (rejectResult is AppResult.Success) setStatus(reservationId, ReservationStatus.REJECTED)
        return rejectResult
    }

    override fun observeUnseenUpdates(): Flow<Int> = unseen

    override suspend fun refreshUnseenUpdates(): AppResult<Unit> {
        calls += "refreshUnseen"
        return AppResult.Success(Unit)
    }

    override suspend fun markUpdatesSeen(): AppResult<Unit> {
        calls += "markUpdatesSeen"
        unseen.value = 0
        return AppResult.Success(Unit)
    }

    override suspend fun clear() {
        calls += "clear"
        mine.value = emptyList()
        company.value = emptyList()
    }

    private fun setStatus(id: String, status: ReservationStatus) {
        company.value = company.value.map { if (it.id == id) it.copy(status = status) else it }
    }
}

// ---------------- Asignaciones ----------------

val testEmployee = AssignmentEmployee(id = "emp-1", name = "Juan Pérez", photoUrl = null)
val testVersa = AssignmentVehicle(
    id = "car-1",
    title = "Nissan Versa 2022",
    plates = "ABC-123",
    color = "Blanco",
    photoUrl = null,
    state = VehicleState.OCCUPIED,
)
val testAveo = AssignmentVehicle(
    id = "car-2",
    title = "Chevrolet Aveo 2020",
    plates = "XYZ-987",
    color = null,
    photoUrl = null,
    state = VehicleState.OCCUPIED,
)

/** Juan Pérez tiene dos carros: el Versa (res-1) y el Aveo (res-2). */
fun testAssignmentsOverview(): AssignmentsOverview {
    val versaAssignment = Assignment(reservationId = "res-1", since = "2026-11-01", hasActiveTrip = false)
    val aveoAssignment = Assignment(reservationId = "res-2", since = "2026-11-05", hasActiveTrip = false)
    return AssignmentsOverview(
        vehicles = listOf(
            VehicleAssignment(testVersa, testEmployee, versaAssignment),
            VehicleAssignment(testAveo, testEmployee, aveoAssignment),
        ),
        employees = listOf(
            EmployeeAssignments(
                testEmployee,
                listOf(EmployeeVehicle(testVersa, versaAssignment), EmployeeVehicle(testAveo, aveoAssignment)),
            ),
        ),
    )
}

/** Quita la asignación [reservationId] (como queda en la API después de desvincular). */
fun AssignmentsOverview.withoutReservation(reservationId: String) = AssignmentsOverview(
    vehicles = vehicles.map { if (it.assignment?.reservationId == reservationId) it.copy(holder = null, assignment = null) else it },
    employees = employees.map { e -> e.copy(vehicles = e.vehicles.filterNot { it.assignment.reservationId == reservationId }) },
)

/**
 * Asignaciones en memoria. [server] es lo que hay en la API; [load] lo copia a lo observado.
 * Desvincular con éxito lo quita de [server] (se ve en la siguiente carga, como en la app).
 */
class FakeAssignmentsRepository(var server: AssignmentsOverview = testAssignmentsOverview()) : AssignmentsRepository {
    private val loaded = MutableStateFlow<AssignmentsOverview?>(null)
    var loadFailure: AppError? = null
    var unlinkResult: AppResult<Unit> = AppResult.Success(Unit)
    var loadCalls = 0
    val unlinked = mutableListOf<Pair<String, String>>()

    override fun observe(): Flow<AssignmentsOverview?> = loaded

    override suspend fun load(): AppResult<AssignmentsOverview> {
        loadCalls++
        loadFailure?.let { return AppResult.Failure(it) }
        loaded.value = server
        return AppResult.Success(server)
    }

    override suspend fun unlink(reservationId: String, reason: String): AppResult<Unit> {
        unlinked += reservationId to reason
        if (unlinkResult is AppResult.Success) server = server.withoutReservation(reservationId)
        return unlinkResult
    }
}
