package org.vicpoo.gasolineproject.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.vicpoo.gasolineproject.core.network.dto.RefreshTokenRequestDto
import org.vicpoo.gasolineproject.core.network.dto.SessionTokensDto

/**
 * Llamadas de la sesión que NO llevan el JWT: renovar y cerrar sesión con el refresh token.
 * Usan un cliente HTTP propio sin AuthPlugin (si usaran el principal, un 401 aquí volvería a
 * intentar renovar sin fin).
 */
interface SessionApi {
    /** POST /api/auth/refresh. Lanza ResponseException (401 = la sesión terminó) o IOException (sin red). */
    suspend fun refresh(refreshToken: String): SessionTokensDto

    /** POST /api/auth/logout. Siempre responde 204. */
    suspend fun logout(refreshToken: String)
}

class KtorSessionApi(private val client: HttpClient) : SessionApi {

    override suspend fun refresh(refreshToken: String): SessionTokensDto =
        client.post(ApiRoutes.REFRESH) {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequestDto(refreshToken))
        }.body()

    override suspend fun logout(refreshToken: String) {
        client.post(ApiRoutes.LOGOUT) {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequestDto(refreshToken))
        }
    }
}
