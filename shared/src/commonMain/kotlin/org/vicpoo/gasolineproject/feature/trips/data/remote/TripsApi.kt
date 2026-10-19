package org.vicpoo.gasolineproject.feature.trips.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.network.ApiRoutes
import org.vicpoo.gasolineproject.feature.auth.data.remote.dto.EmpleadoDto

/** ViajeDto de APIGasoline (GET /api/viajes). Fechas en hora local del servidor. */
@Serializable
data class ViajeDto(
    val viajeId: String,
    val empresaId: String,
    val carroId: String,
    val empleadoOperadorId: String,
    val fechaInicio: String,
    val fechaFin: String? = null,
    val kilometrajeInicial: Double = 0.0,
    val kilometrajeFinal: Double? = null,
    val distanciaRecorrida: Double? = null,
    /** "enCurso" | "finalizado" | "interrumpido" */
    val estado: String? = null,
)

/** CargaCombustibleDto (GET /api/cargas-combustible). */
@Serializable
data class CargaCombustibleDto(
    val cargaCombustibleId: String,
    val viajeId: String,
    val litros: Double = 0.0,
    val monto: Double? = null,
    val fotoComprobanteUrl: String? = null,
    val fechaCarga: String? = null,
)

@Serializable
data class CoordenadaDto(val latitud: Double, val longitud: Double)

/** MapaRutaDto (GET /api/viajes/{id}/ruta-procesada/mapa). */
@Serializable
data class MapaRutaDto(
    val viajeId: String,
    val coordenadas: List<CoordenadaDto> = emptyList(),
)

interface TripsApi {
    suspend fun getTrips(companyId: String?, operatorId: String?): List<ViajeDto>
    suspend fun getFuelLoads(companyId: String?): List<CargaCombustibleDto>
    suspend fun getRouteMap(tripId: String): MapaRutaDto
    suspend fun getEmployees(companyId: String): List<EmpleadoDto>
}

class KtorTripsApi(private val client: HttpClient) : TripsApi {

    override suspend fun getTrips(companyId: String?, operatorId: String?): List<ViajeDto> =
        client.get(ApiRoutes.TRIPS) {
            companyId?.let { parameter("empresaId", it) }
            operatorId?.let { parameter("empleadoOperadorId", it) }
        }.body()

    override suspend fun getFuelLoads(companyId: String?): List<CargaCombustibleDto> =
        client.get(ApiRoutes.FUEL_LOADS) {
            companyId?.let { parameter("empresaId", it) }
        }.body()

    override suspend fun getRouteMap(tripId: String): MapaRutaDto =
        client.get(ApiRoutes.tripRouteMap(tripId)).body()

    override suspend fun getEmployees(companyId: String): List<EmpleadoDto> =
        client.get(ApiRoutes.EMPLOYEES) { parameter("empresaId", companyId) }.body()
}
