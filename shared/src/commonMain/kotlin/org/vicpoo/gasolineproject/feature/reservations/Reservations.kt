package org.vicpoo.gasolineproject.feature.reservations

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.network.ApiRoutes
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.session.SessionReader

// ======================= Data =======================

/**
 * CreateSolicitudReservaRequest de APIGasoline (POST /api/solicitudes-reserva).
 * "motivo" todavía NO existe en la API: se manda para que funcione en cuanto se agregue
 * (ASP.NET ignora los campos que no conoce).
 */
@Serializable
data class CreateReservationRequestDto(
    val empresaId: String,
    val carroId: String,
    val empleadoSolicitanteId: String,
    val motivo: String? = null,
)

interface ReservationApi {
    suspend fun create(request: CreateReservationRequestDto)
}

class KtorReservationApi(private val client: HttpClient) : ReservationApi {
    override suspend fun create(request: CreateReservationRequestDto) {
        client.post(ApiRoutes.RESERVATION_REQUESTS) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}

// ======================= Domain =======================

interface ReservationRepository {
    /** Solicitar un carro. Requiere conexión: la cola de confirmación vive en el servidor. */
    suspend fun requestVehicle(vehicleServerId: String, reason: String): AppResult<Unit>
}

class ReservationRepositoryImpl(
    private val api: ReservationApi,
    private val sessionReader: SessionReader,
) : ReservationRepository {

    override suspend fun requestVehicle(vehicleServerId: String, reason: String): AppResult<Unit> {
        val session = sessionReader.current() ?: return AppResult.Failure(AppError.Unauthorized)
        val employeeId = session.employeeId ?: return AppResult.Failure(AppError.NoCompany)
        val companyId = session.companyId ?: return AppResult.Failure(AppError.NoCompany)
        return safeApiCall {
            api.create(
                CreateReservationRequestDto(
                    empresaId = companyId,
                    carroId = vehicleServerId,
                    empleadoSolicitanteId = employeeId,
                    motivo = reason,
                ),
            )
        }
    }
}

/** Valida el motivo y envía la solicitud. */
class RequestVehicleUseCase(private val repository: ReservationRepository) {

    suspend operator fun invoke(vehicleServerId: String?, reason: String): AppResult<Unit> {
        val clean = reason.trim()
        if (clean.isEmpty()) return AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED))
        if (clean.length > MAX_REASON) return AppResult.Failure(AppError.Validation(ValidationReason.OUT_OF_RANGE))
        // Un carro creado sin conexión aún no existe en la API
        if (vehicleServerId == null) return AppResult.Failure(AppError.NotFound)
        return repository.requestVehicle(vehicleServerId, clean)
    }

    companion object {
        const val MAX_REASON = 255
    }
}

// ======================= DI =======================

val reservationsModule = module {
    single<ReservationApi> { KtorReservationApi(get()) }
    single<ReservationRepository> { ReservationRepositoryImpl(api = get(), sessionReader = get()) }
    factoryOf(::RequestVehicleUseCase)
}
