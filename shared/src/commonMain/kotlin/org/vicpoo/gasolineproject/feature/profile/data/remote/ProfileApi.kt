package org.vicpoo.gasolineproject.feature.profile.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.network.ApiRoutes
import org.vicpoo.gasolineproject.core.network.singleFileMultipart
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.feature.auth.data.remote.dto.EmpleadoDto
import org.vicpoo.gasolineproject.feature.auth.data.remote.dto.PerfilUsuarioDto

/** UpdateEmpleadoRequest de APIGasoline (PUT /api/empleados/{id}). */
@Serializable
data class UpdateEmpleadoRequestDto(
    val empresaId: String,
    val usuarioId: String,
    val nombreCompleto: String,
)

/** Fuente remota del perfil (AuthController + EmpleadosController). */
interface ProfileApi {
    /** GET /api/auth/perfil */
    suspend fun getProfile(): PerfilUsuarioDto

    /** PUT /api/empleados/{empleadoId} */
    suspend fun updateEmployee(employeeId: String, request: UpdateEmpleadoRequestDto): EmpleadoDto

    /** PUT /api/empleados/{empleadoId}/foto-perfil (multipart, jpg/png/webp, máx. 5 MB) */
    suspend fun uploadProfilePhoto(employeeId: String, file: PickedFile): EmpleadoDto
}

class KtorProfileApi(private val client: HttpClient) : ProfileApi {

    override suspend fun getProfile(): PerfilUsuarioDto =
        client.get(ApiRoutes.PROFILE).body()

    override suspend fun updateEmployee(employeeId: String, request: UpdateEmpleadoRequestDto): EmpleadoDto =
        client.put(ApiRoutes.employee(employeeId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun uploadProfilePhoto(employeeId: String, file: PickedFile): EmpleadoDto =
        client.put(ApiRoutes.employeeProfilePhoto(employeeId)) {
            setBody(singleFileMultipart(file.bytes, file.name, file.mimeType))
        }.body()
}
