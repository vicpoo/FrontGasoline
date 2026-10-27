package org.vicpoo.gasolineproject.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Motor HTTP de cada plataforma (OkHttp en Android, Darwin en iOS). */
expect fun httpEngine(): HttpClientEngine

/** Quién da el token actual. Lo implementa la sesión; el cliente HTTP no sabe de dónde viene. */
fun interface TokenProvider {
    suspend fun token(): String?
}

/** Qué hacer cuando la API responde 401 (token vencido o inválido). */
fun interface UnauthorizedHandler {
    suspend fun onUnauthorized()
}

class AuthConfig {
    var tokenProvider: TokenProvider = TokenProvider { null }
    var unauthorizedHandler: UnauthorizedHandler = UnauthorizedHandler { }
}

/**
 * - Agrega "Authorization: Bearer <token>" a cada petición si hay sesión.
 * - Si una petición con token responde 401, cierra la sesión (la app regresa a la bienvenida).
 */
val AuthPlugin = createClientPlugin("AuthPlugin", ::AuthConfig) {
    val provider = pluginConfig.tokenProvider
    val handler = pluginConfig.unauthorizedHandler

    onRequest { request, _ ->
        val token = provider.token()
        if (!token.isNullOrBlank() && !request.headers.contains(HttpHeaders.Authorization)) {
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    onResponse { response ->
        val sentToken = response.call.request.headers.contains(HttpHeaders.Authorization)
        if (response.status == HttpStatusCode.Unauthorized && sentToken) {
            handler.onUnauthorized()
        }
    }
}

fun createHttpClient(
    engine: HttpClientEngine,
    tokenProvider: TokenProvider,
    unauthorizedHandler: UnauthorizedHandler,
    json: Json,
): HttpClient = HttpClient(engine) {
    expectSuccess = true // 4xx/5xx lanzan excepción y safeApiCall las convierte en AppError
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        requestTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
        connectTimeoutMillis = ApiConfig.CONNECT_TIMEOUT_MS
    }
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.INFO
    }
    install(AuthPlugin) {
        this.tokenProvider = tokenProvider
        this.unauthorizedHandler = unauthorizedHandler
    }
    defaultRequest {
        url(ApiConfig.BASE_URL)
        // La API guarda el origen en la auditoría (movil | web)
        headers.append(ApiConfig.ORIGIN_HEADER, ApiConfig.ORIGIN_MOBILE)
    }
}
