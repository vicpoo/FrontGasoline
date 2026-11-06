package org.vicpoo.gasolineproject.data.network

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.data.network.dto.SessionTokensDto
import org.vicpoo.gasolineproject.domain.session.AuthTokens
import org.vicpoo.gasolineproject.domain.session.SessionWriter
import org.vicpoo.gasolineproject.domain.session.TokenExpiry
import org.vicpoo.gasolineproject.domain.session.TokenStore

/** Resultado de intentar renovar la sesión. */
sealed interface RefreshResult {
    /** Hay un JWT nuevo (lo renovó esta llamada u otra que iba antes). */
    data class Renewed(val accessToken: String) : RefreshResult

    /** La API rechazó el refresh token (401) o ya venció: hay que iniciar sesión de nuevo. */
    data object SessionEnded : RefreshResult

    /** No se pudo preguntar (sin red, timeout, 5xx, 429): la sesión sigue; se reintenta después. */
    data object Unavailable : RefreshResult
}

/** Lo que usa el AuthPlugin cuando una petición responde 401. */
fun interface TokenRefresher {
    /** [failedAccessToken] = el JWT con el que la API respondió 401. */
    suspend fun refreshAfterUnauthorized(failedAccessToken: String): RefreshResult

    companion object {
        /** Sin renovación (pruebas): un 401 se queda como 401. */
        val None = TokenRefresher { RefreshResult.SessionEnded }
    }
}

/**
 * Dueño de los tokens de la sesión:
 * - Da el JWT para cada petición y lo renueva ANTES de que venza (si vence en menos de 1 min).
 * - Si una petición responde 401, renueva con el refresh token y la petición se reintenta (AuthPlugin).
 * - Una sola renovación a la vez ([mutex]): si 5 peticiones reciben 401 juntas, solo la primera llama
 *   a la API; las demás ven que el JWT ya cambió y usan el nuevo. Esto es obligatorio con la rotación:
 *   dos renovaciones con el mismo refresh token harían que la API lo tome como robo.
 * - Si la API rechaza el refresh token (401), cierra la sesión local (la app regresa al login).
 * - Sin red NO cierra la sesión: la app sigue funcionando offline y renueva al volver la conexión.
 */
class AuthTokenManager(
    private val store: TokenStore,
    private val api: SessionApi,
    private val sessionWriter: SessionWriter,
    private val nowMillis: () -> Long,
) : TokenProvider, TokenRefresher {

    private val mutex = Mutex()

    /** JWT para la siguiente petición (renovado si estaba por vencer y hay red). null = sin sesión. */
    override suspend fun token(): String? {
        val tokens = store.read() ?: return null
        if (tokens.refreshToken == null ||
            !TokenExpiry.expiresWithin(tokens.accessExpiresAtUtc, nowMillis(), PROACTIVE_MARGIN_MS)
        ) {
            return tokens.accessToken
        }
        return when (val result = refresh(tokens.accessToken)) {
            is RefreshResult.Renewed -> result.accessToken
            RefreshResult.SessionEnded -> null
            // Sin red: se manda el actual; si ya venció, la API responde 401 y se reintenta luego
            RefreshResult.Unavailable -> tokens.accessToken
        }
    }

    override suspend fun refreshAfterUnauthorized(failedAccessToken: String): RefreshResult = refresh(failedAccessToken)

    /** Guarda los tokens de un login nuevo. */
    suspend fun saveNewSession(tokens: AuthTokens) = mutex.withLock { store.write(tokens) }

    /** Tokens actuales (para el logout remoto). */
    suspend fun currentTokens(): AuthTokens? = store.read()

    /**
     * Borra los tokens. Pasa por el mismo candado que la renovación: así una renovación que
     * estaba en curso no puede volver a guardar tokens después de cerrar sesión.
     */
    suspend fun clear() = mutex.withLock { store.clear() }

    private suspend fun refresh(failedAccessToken: String): RefreshResult = mutex.withLock {
        val current = store.read() ?: return@withLock RefreshResult.SessionEnded

        // Otra petición ya renovó mientras esta esperaba el candado
        if (current.accessToken != failedAccessToken) return@withLock RefreshResult.Renewed(current.accessToken)

        val refreshToken = current.refreshToken
        if (refreshToken == null || TokenExpiry.expiresWithin(current.refreshExpiresAtUtc, nowMillis())) {
            endSession()
            return@withLock RefreshResult.SessionEnded
        }

        try {
            val renewed = current.renewedWith(api.refresh(refreshToken))
            store.write(renewed)
            RefreshResult.Renewed(renewed.accessToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                endSession()
                RefreshResult.SessionEnded
            } else {
                RefreshResult.Unavailable // 429, 5xx...: la sesión sigue
            }
        } catch (e: Exception) {
            RefreshResult.Unavailable // sin red, timeout, respuesta ilegible
        }
    }

    /** Llamar SOLO con el candado tomado. */
    private suspend fun endSession() {
        println("[API] La sesión terminó (refresh token rechazado o vencido): se pide iniciar sesión.")
        store.clear()
        sessionWriter.clear() // la app regresa a la bienvenida (ObserveIsLoggedIn)
    }

    private fun AuthTokens.renewedWith(dto: SessionTokensDto) = copy(
        accessToken = dto.token,
        accessExpiresAtUtc = dto.expiraEnUtc,
        refreshToken = dto.refreshToken,
        refreshExpiresAtUtc = dto.refreshExpiraEnUtc ?: refreshExpiresAtUtc,
        sessionId = dto.sesionId ?: sessionId,
    )

    private companion object {
        /** Se renueva si al JWT le queda menos de esto. */
        const val PROACTIVE_MARGIN_MS = 60_000L
    }
}
