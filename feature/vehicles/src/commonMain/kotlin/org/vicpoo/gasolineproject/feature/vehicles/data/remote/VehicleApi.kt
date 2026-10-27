package org.vicpoo.gasolineproject.feature.vehicles.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.network.ApiRoutes
import org.vicpoo.gasolineproject.core.network.singleFileMultipart

/** CarroDto de APIGasoline (GET /api/carros). Ids Guid -> String; decimal -> Double; enums en camelCase. */
@Serializable
data class CarroDto(
    val carroId: String,
    val empresaId: String,
    val marca: String,
    val modelo: String,
    val anio: Int? = null,
    val color: String? = null,
    val placas: String,
    val fotoVehiculoUrl: String? = null,
    val capacidadTanque: Double? = null,
    val kilometrajeActual: Double = 0.0,
    /** "disponible" | "ocupado" | "enMantenimiento" */
    val estado: String? = null,
    val dadoDeBaja: Boolean = false,
)

/** CreateCarroRequest / UpdateCarroRequest (Update ignora "estado"). */
@Serializable
data class CarroRequestDto(
    val empresaId: String,
    val marca: String,
    val modelo: String,
    val anio: Int?,
    val color: String?,
    val placas: String,
    val capacidadTanque: Double?,
    val kilometrajeActual: Double,
)

/** Fuente remota de vehículos (CarrosController). */
interface VehicleApi {
    suspend fun getAll(companyId: String?): List<CarroDto>
    suspend fun create(request: CarroRequestDto): CarroDto
    suspend fun update(id: String, request: CarroRequestDto): CarroDto
    suspend fun delete(id: String)
    suspend fun uploadPhoto(id: String, bytes: ByteArray, fileName: String, mimeType: String): CarroDto
    suspend fun deletePhoto(id: String)
}

class KtorVehicleApi(private val client: HttpClient) : VehicleApi {

    /** Los empleados solo ven los carros de su empresa (?empresaId=...). */
    override suspend fun getAll(companyId: String?): List<CarroDto> =
        client.get(ApiRoutes.VEHICLES) {
            if (companyId != null) parameter("empresaId", companyId)
        }.body()

    override suspend fun create(request: CarroRequestDto): CarroDto =
        client.post(ApiRoutes.VEHICLES) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun update(id: String, request: CarroRequestDto): CarroDto =
        client.put(ApiRoutes.vehicle(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun delete(id: String) {
        client.delete(ApiRoutes.vehicle(id))
    }

    /** PUT /api/carros/{id}/foto (multipart, campo "archivo": jpg, png o webp, máx. 5 MB). */
    override suspend fun uploadPhoto(id: String, bytes: ByteArray, fileName: String, mimeType: String): CarroDto =
        client.put(ApiRoutes.vehiclePhoto(id)) {
            setBody(singleFileMultipart(bytes, fileName, mimeType))
        }.body()

    override suspend fun deletePhoto(id: String) {
        client.delete(ApiRoutes.vehiclePhoto(id))
    }
}
