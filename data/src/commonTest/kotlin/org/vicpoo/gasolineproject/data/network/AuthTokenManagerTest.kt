package org.vicpoo.gasolineproject.data.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.session.AuthTokens
import org.vicpoo.gasolineproject.domain.session.InMemoryTokenStore
import org.vicpoo.gasolineproject.domain.session.TokenExpiry
import org.vicpoo.gasolineproject.testing.FakeApiServer
import org.vicpoo.gasolineproject.testing.FakeSessionStore
import org.vicpoo.gasolineproject.testing.noConnection
import org.vicpoo.gasolineproject.testing.respondJson
import org.vicpoo.gasolineproject.testing.respondProblem
import org.vicpoo.gasolineproject.testing.testJson
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Sesión larga: el JWT se renueva solo (antes de vencer o al recibir 401) con el refresh token,
 * una sola renovación a la vez, y la sesión se cierra SOLO si la API rechaza el refresh token.
 */
class AuthTokenManagerTest {

    private val now = TokenExpiry.epochMillis("2026-11-06T12:00:00Z")!!
    private var clock = now

    private val oldTokens = AuthTokens(
        accessToken = "jwt-viejo",
        accessExpiresAtUtc = "2026-11-06T12:10:00Z",
        refreshToken = "refresh-1",
        refreshExpiresAtUtc = "2026-12-06T12:00:00Z",
        sessionId = "sesion-1",
    )

    private val store = InMemoryTokenStore(oldTokens)
    private val sessions = FakeSessionStore(testSession())

    /** Respuesta del POST /auth/refresh (cámbiala en cada prueba). */
    private var refreshResponse: suspend io.ktor.client.engine.mock.MockRequestHandleScope.() -> io.ktor.client.request.HttpResponseData = {
        respondJson(
            """{"token":"jwt-nuevo","tipoToken":"Bearer","expiraEnUtc":"2026-11-06T12:25:00Z",
               "refreshToken":"refresh-2","refreshExpiraEnUtc":"2026-12-06T12:15:00Z","sesionId":"sesion-1"}""",
        )
    }
    private var refreshCalls = 0

    private val sessionApi = KtorSessionApi(
        createSessionHttpClient(
            MockEngine {
                refreshCalls++
                refreshResponse()
            },
            testJson,
        ),
    )

    private val manager = AuthTokenManager(store, sessionApi, sessions, nowMillis = { clock })

    /** API que solo acepta el JWT nuevo. */
    private val server = FakeApiServer(tokenProvider = manager, tokenRefresher = manager).apply {
        handler = { request ->
            when (request.headers[HttpHeaders.Authorization]) {
                "Bearer jwt-nuevo" -> respondJson("[]")
                else -> respondProblem(HttpStatusCode.Unauthorized, "Token vencido")
            }
        }
    }

    private suspend fun getVehicles(): AppResult<String> = safeApiCall { server.client.get("carros").bodyAsText() }

    @Test
    fun token_vigente_seUsaSinRenovar() = runTest {
        assertEquals("jwt-viejo", manager.token())
        assertEquals(0, refreshCalls)
    }

    @Test
    fun token_porVencer_seRenuevaAntes() = runTest {
        clock = now + 9 * 60_000 + 30_000 // le quedan 30 s

        assertEquals("jwt-nuevo", manager.token())
        assertEquals(1, refreshCalls)
        assertEquals("refresh-2", store.tokens?.refreshToken)
    }

    @Test
    fun respuesta401_renuevaYReintentaLaPeticion() = runTest {
        val result = getVehicles()

        assertEquals(AppResult.Success("[]"), result)
        assertEquals(1, refreshCalls)
        assertEquals("jwt-nuevo", store.tokens?.accessToken)
        assertEquals("refresh-2", store.tokens?.refreshToken) // rotación
        assertNotNull(sessions.state.value) // la sesión sigue
    }

    @Test
    fun variosPeticiones401AlMismoTiempo_unaSolaRenovacion() = runTest {
        val results = List(5) { async { getVehicles() } }.awaitAll()

        results.forEach { assertEquals(AppResult.Success("[]"), it) }
        assertEquals(1, refreshCalls)
    }

    @Test
    fun refreshRechazado_cierraLaSesion() = runTest {
        refreshResponse = { respondProblem(HttpStatusCode.Unauthorized, "La sesión no es válida") }

        val result = getVehicles()

        assertEquals(AppResult.Failure(AppError.Unauthorized), result)
        assertNull(store.tokens)
        assertNull(sessions.state.value) // la app regresa al login
    }

    @Test
    fun sinRedParaRenovar_noCierraLaSesion() = runTest {
        refreshResponse = { noConnection() }

        val result = getVehicles()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        assertEquals(oldTokens, store.tokens)
        assertNotNull(sessions.state.value)
    }

    @Test
    fun errorDelServidorAlRenovar_noCierraLaSesion() = runTest {
        refreshResponse = { respondProblem(HttpStatusCode.ServiceUnavailable, "Mantenimiento") }

        val result = getVehicles()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        assertEquals(oldTokens, store.tokens)
    }

    @Test
    fun refreshVencido_cierraLaSesionSinLlamarALaApi() = runTest {
        clock = TokenExpiry.epochMillis("2026-12-07T00:00:00Z")!!

        val result = manager.refreshAfterUnauthorized("jwt-viejo")

        assertIs<RefreshResult.SessionEnded>(result)
        assertEquals(0, refreshCalls)
        assertNull(sessions.state.value)
    }

    @Test
    fun otraPeticionYaRenovo_seUsaElTokenNuevoSinLlamarALaApi() = runTest {
        store.write(oldTokens.copy(accessToken = "jwt-nuevo"))

        val result = manager.refreshAfterUnauthorized("jwt-viejo")

        assertEquals(RefreshResult.Renewed("jwt-nuevo"), result)
        assertEquals(0, refreshCalls)
    }

    @Test
    fun sinSesion_noMandaToken() = runTest {
        store.clear()

        assertNull(manager.token())
    }

    @Test
    fun cerrarSesion_borraLosTokens() = runTest {
        manager.clear()

        assertNull(store.tokens)
    }
}
