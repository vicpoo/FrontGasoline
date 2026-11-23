package org.vicpoo.gasolineproject.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import org.vicpoo.gasolineproject.domain.common.AppLogLevel
import org.vicpoo.gasolineproject.domain.common.AppLogger
import org.vicpoo.gasolineproject.domain.common.SilentAppLogger
import org.vicpoo.gasolineproject.domain.common.d

/** Motor HTTP de cada plataforma (OkHttp en Android, Darwin en iOS). */
expect fun httpEngine(): HttpClientEngine

/** Quién da el token actual. Lo implementa la sesión; el cliente HTTP no sabe de dónde viene. */
fun interface TokenProvider {
    suspend fun token(): String?
}

/**
 * La petición respondió 401 y no se pudo renovar la sesión porque no hay red (o la API no respondió).
 * Es una IOException para que safeApiCall la trate como "sin conexión": la sesión NO se cierra y la
 * sincronización se reintenta después.
 */
class SessionRefreshUnavailableException : IOException("No se pudo renovar la sesión (sin conexión)")

class AuthConfig {
    var tokenProvider: TokenProvider = TokenProvider { null }
    var tokenRefresher: TokenRefresher = TokenRefresher.None
}

/**
 * - Agrega "Authorization: Bearer <token>" a cada petición si hay sesión (el TokenProvider lo
 *   renueva antes de que venza).
 * - Si una petición con token responde 401, renueva la sesión con el refresh token y la REINTENTA
 *   una vez con el JWT nuevo (el usuario no se entera).
 *   - Si la API rechaza el refresh token, el TokenRefresher cierra la sesión y se regresa el 401.
 *   - Si no hay red para renovar, se lanza [SessionRefreshUnavailableException] (= sin conexión).
 */
val AuthPlugin = createClientPlugin("AuthPlugin", ::AuthConfig) {
    val provider = pluginConfig.tokenProvider
    val refresher = pluginConfig.tokenRefresher

    onRequest { request, _ ->
        if (request.headers.contains(HttpHeaders.Authorization)) return@onRequest
        val token = provider.token()
        if (!token.isNullOrBlank()) {
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    on(Send) { request ->
        val sentToken = request.bearerToken() ?: return@on proceed(request)

        // Con expectSuccess el 401 puede llegar como excepción o como respuesta, según el orden de los plugins
        var unauthorized: ResponseException? = null
        val firstCall: HttpClientCall? = try {
            proceed(request)
        } catch (e: ResponseException) {
            if (e.response.status != HttpStatusCode.Unauthorized) throw e
            unauthorized = e
            null
        }
        if (firstCall != null && firstCall.response.status != HttpStatusCode.Unauthorized) return@on firstCall

        when (val result = refresher.refreshAfterUnauthorized(sentToken)) {
            is RefreshResult.Renewed -> {
                val retry = HttpRequestBuilder().takeFrom(request)
                retry.headers[HttpHeaders.Authorization] = "Bearer ${result.accessToken}"
                proceed(retry)
            }
            RefreshResult.Unavailable -> throw SessionRefreshUnavailableException()
            // Sin 401 como excepción, el 401 llegó como respuesta: se regresa tal cual
            RefreshResult.SessionEnded -> unauthorized?.let { throw it }
                ?: checkNotNull(firstCall) { "Sin respuesta ni excepción después de un 401" }
        }
    }
}

private fun HttpRequestBuilder.bearerToken(): String? =
    headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substring("Bearer ".length)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

/**
 * [appLogger]: solo en debug se registran las peticiones (método, URL y código, sin encabezados ni cuerpo).
 * En release el nivel mínimo es WARN y el plugin de Logging ni se instala.
 */
fun createHttpClient(
    engine: HttpClientEngine,
    baseUrl: String,
    tokenProvider: TokenProvider,
    tokenRefresher: TokenRefresher,
    json: Json,
    appLogger: AppLogger = SilentAppLogger,
): HttpClient = HttpClient(engine) {
    expectSuccess = true // 4xx/5xx lanzan excepción y safeApiCall las convierte en AppError
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        requestTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
        connectTimeoutMillis = ApiConfig.CONNECT_TIMEOUT_MS
    }
    if (appLogger.isEnabled(AppLogLevel.DEBUG)) {
        install(Logging) {
            this.logger = object : Logger {
                override fun log(message: String) = appLogger.d("HTTP", message)
            }
            // INFO = método, URL y código de respuesta; nunca encabezados (Authorization) ni cuerpos (login, fotos)
            level = LogLevel.INFO
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }
    install(AuthPlugin) {
        this.tokenProvider = tokenProvider
        this.tokenRefresher = tokenRefresher
    }
    defaultRequest {
        url(baseUrl)
        // La API guarda el origen en la auditoría (movil | web)
        headers.append(ApiConfig.ORIGIN_HEADER, ApiConfig.ORIGIN_MOBILE)
    }
}

/**
 * Cliente para renovar / cerrar la sesión (SessionApi): mismo JSON, tiempos y origen, pero SIN
 * AuthPlugin (no lleva el JWT y un 401 aquí significa "la sesión terminó", no "renueva").
 */
fun createSessionHttpClient(engine: HttpClientEngine, baseUrl: String, json: Json): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        requestTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
        connectTimeoutMillis = ApiConfig.CONNECT_TIMEOUT_MS
    }
    defaultRequest {
        url(baseUrl)
        headers.append(ApiConfig.ORIGIN_HEADER, ApiConfig.ORIGIN_MOBILE)
    }
}
