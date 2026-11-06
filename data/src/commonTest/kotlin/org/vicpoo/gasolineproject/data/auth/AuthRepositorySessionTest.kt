package org.vicpoo.gasolineproject.data.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.data.network.AuthTokenManager
import org.vicpoo.gasolineproject.data.network.KtorSessionApi
import org.vicpoo.gasolineproject.data.network.createSessionHttpClient
import org.vicpoo.gasolineproject.domain.session.AuthTokens
import org.vicpoo.gasolineproject.domain.session.InMemoryTokenStore
import org.vicpoo.gasolineproject.domain.session.LogoutListener
import org.vicpoo.gasolineproject.data.auth.remote.KtorAuthApi
import org.vicpoo.gasolineproject.data.auth.repository.AuthRepositoryImpl
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
import kotlin.test.assertNull

/** Login guarda los tokens aparte (cifrados) y logout revoca el refresh token en la API. */
class AuthRepositorySessionTest {

    private val store = InMemoryTokenStore()
    private val sessions = FakeSessionStore()
    private val sessionRequests = mutableListOf<String>()
    private var logoutOffline = false

    private val sessionApi = KtorSessionApi(
        createSessionHttpClient(
            MockEngine { request ->
                sessionRequests += request.url.encodedPath.removePrefix("/api/")
                if (logoutOffline) noConnection()
                respond("", HttpStatusCode.NoContent)
            },
            testJson,
        ),
    )

    private val tokenManager = AuthTokenManager(store, sessionApi, sessions, nowMillis = { 0L })
    private val server = FakeApiServer(tokenProvider = tokenManager, tokenRefresher = tokenManager)
    private val listenerCalls = mutableListOf<String>()

    private val repository = AuthRepositoryImpl(
        api = KtorAuthApi(server.client),
        sessionReader = sessions,
        sessionWriter = sessions,
        tokenManager = tokenManager,
        sessionApi = sessionApi,
        logoutListeners = listOf(LogoutListener { listenerCalls += "push" }),
        device = AuthRepositoryImpl.DeviceInfo(platform = "android", name = "Pixel 8"),
    )

    @Test
    fun login_guardaLosTokensAparteDeLaSesion() = runTest {
        server.handler = {
            respondJson(
                """{"token":"jwt","tipoToken":"Bearer","expiraEnUtc":"2026-11-06T12:15:00Z",
                   "refreshToken":"refresh-1","refreshExpiraEnUtc":"2026-12-06T12:00:00Z","sesionId":"s-1",
                   "perfil":{"usuario":{"usuarioId":"u-1","email":"chofer@empresa.com"},
                             "empleado":{"empleadoId":"e-1","empresaId":"c-1","tieneLicencia":true},
                             "roles":["Operador"],"privilegios":["viaje.registrar"],"versionPrivilegios":"v1"}}""",
            )
        }

        val result = repository.login("chofer@empresa.com", "Gasolina2026")

        assertIs<AppResult.Success<*>>(result)
        assertEquals(
            AuthTokens("jwt", "2026-11-06T12:15:00Z", "refresh-1", "2026-12-06T12:00:00Z", "s-1"),
            store.tokens,
        )
        assertEquals("u-1", sessions.state.value?.userId)
    }

    private val loginOk = """{"token":"jwt","tipoToken":"Bearer","expiraEnUtc":"2026-11-06T12:15:00Z",
        "refreshToken":"refresh-1","refreshExpiraEnUtc":"2026-12-06T12:00:00Z","sesionId":"s-1",
        "perfil":{"usuario":{"usuarioId":"u-2","email":"otra@empresa.com"},
                  "empleado":{"empleadoId":"e-2","empresaId":"c-1","tieneLicencia":true},
                  "roles":["Operador"],"privilegios":["viaje.registrar"],"versionPrivilegios":"v1"}}"""

    @Test
    fun loginDeOtraCuenta_borraLosDatosDelTelefonoAntesDeGuardarLaSesion() = runTest {
        val sessionWhenCleared = mutableListOf<String?>()
        val repository = AuthRepositoryImpl(
            api = KtorAuthApi(server.client),
            sessionReader = sessions,
            sessionWriter = sessions,
            tokenManager = tokenManager,
            sessionApi = sessionApi,
            logoutListeners = listOf(LogoutListener { sessionWhenCleared += sessions.state.value?.userId }),
        )
        server.handler = { respondJson(loginOk) }

        repository.login("otra@empresa.com", "x", clearLocalData = true)

        assertEquals(listOf<String?>(null), sessionWhenCleared) // se borró ANTES de guardar la sesión nueva
        assertEquals("u-2", sessions.state.value?.userId)
    }

    @Test
    fun loginDeOtraCuentaConContrasenaIncorrecta_noBorraNada() = runTest {
        server.handler = { respondProblem(HttpStatusCode.Unauthorized, "Credenciales incorrectas") }

        repository.login("otra@empresa.com", "mala", clearLocalData = true)

        assertEquals(emptyList(), listenerCalls)
    }

    @Test
    fun loginDeLaMismaCuenta_noBorraNada() = runTest {
        server.handler = { respondJson(loginOk) }

        repository.login("otra@empresa.com", "x")

        assertEquals(emptyList(), listenerCalls)
    }

    @Test
    fun logout_revocaElRefreshTokenYBorraTodo() = runTest {
        sessions.save(testSession())
        store.write(AuthTokens("jwt", null, "refresh-1", null))

        repository.logout()

        assertEquals(listOf("push"), listenerCalls) // las features limpian ANTES de revocar
        assertEquals(listOf("auth/logout"), sessionRequests)
        assertNull(store.tokens)
        assertNull(sessions.state.value)
    }

    @Test
    fun logout_sinRed_cierraIgualEnElTelefono() = runTest {
        logoutOffline = true
        sessions.save(testSession())
        store.write(AuthTokens("jwt", null, "refresh-1", null))

        repository.logout()

        assertNull(store.tokens)
        assertNull(sessions.state.value)
    }
}
