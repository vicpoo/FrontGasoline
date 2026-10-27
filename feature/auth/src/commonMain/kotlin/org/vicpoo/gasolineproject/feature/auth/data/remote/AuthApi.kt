package org.vicpoo.gasolineproject.feature.auth.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.vicpoo.gasolineproject.core.network.ApiRoutes
import org.vicpoo.gasolineproject.core.network.singleFileMultipart
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.feature.auth.data.remote.dto.LoginRequestDto
import org.vicpoo.gasolineproject.feature.auth.data.remote.dto.LoginResponseDto
import org.vicpoo.gasolineproject.feature.auth.data.remote.dto.PrivilegiosUsuarioDto

/** Fuente remota de autenticación (solo habla HTTP, sin lógica de negocio). */
interface AuthApi {
    suspend fun login(request: LoginRequestDto): LoginResponseDto

    /** PUT /api/empleados/{empleadoId}/foto-licencia (multipart, campo "archivo"). */
    suspend fun uploadLicense(employeeId: String, file: PickedFile)

    /**
     * GET /api/auth/privilegios con If-None-Match.
     * Regresa null si la API responde 304 (los privilegios no cambiaron).
     */
    suspend fun getPrivileges(currentVersion: String?): PrivilegiosUsuarioDto?
}

class KtorAuthApi(private val client: HttpClient) : AuthApi {

    override suspend fun login(request: LoginRequestDto): LoginResponseDto =
        client.post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun uploadLicense(employeeId: String, file: PickedFile) {
        client.put(ApiRoutes.employeeLicense(employeeId)) {
            setBody(singleFileMultipart(file.bytes, file.name, file.mimeType))
        }
    }

    override suspend fun getPrivileges(currentVersion: String?): PrivilegiosUsuarioDto? {
        val response = client.get(ApiRoutes.PRIVILEGES) {
            // 304 no es error: se revisa a mano
            expectSuccess = false
            if (!currentVersion.isNullOrBlank()) header(HttpHeaders.IfNoneMatch, "\"$currentVersion\"")
        }
        return when (response.status) {
            HttpStatusCode.NotModified -> null
            HttpStatusCode.OK -> response.body()
            else -> throw ClientRequestException(response, "HTTP ${response.status.value}")
        }
    }
}
