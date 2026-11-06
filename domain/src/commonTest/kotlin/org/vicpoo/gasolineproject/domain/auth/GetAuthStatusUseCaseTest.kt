package org.vicpoo.gasolineproject.domain.auth

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.session.AuthTokens
import org.vicpoo.gasolineproject.domain.session.InMemoryTokenStore
import org.vicpoo.gasolineproject.domain.session.TokenExpiry
import org.vicpoo.gasolineproject.domain.auth.usecase.AuthStatus
import org.vicpoo.gasolineproject.domain.auth.usecase.GetAuthStatusUseCase
import org.vicpoo.gasolineproject.testing.FakeLocalDataOwnerStore
import org.vicpoo.gasolineproject.testing.FakeSessionStore
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Al abrir la app (aun sin red): manda la expiración del refresh token, no la del JWT. */
class GetAuthStatusUseCaseTest {

    private val tokens = AuthTokens(
        accessToken = "jwt",
        accessExpiresAtUtc = "2026-11-06T12:15:00Z",
        refreshToken = "refresh",
        refreshExpiresAtUtc = "2026-12-06T12:00:00Z",
    )

    private class FixedClock(private val millis: Long) : AppClock {
        override fun nowMillis(): Long = millis
        override fun toLocalIso(epochMillis: Long): String = ""
    }

    private fun at(iso: String) = FixedClock(TokenExpiry.epochMillis(iso)!!)

    @Test
    fun jwtVencidoPeroRefreshVigente_entraSinPedirLogin() = runTest {
        val sessions = FakeSessionStore(testSession())
        val store = InMemoryTokenStore(tokens)

        val status = GetAuthStatusUseCase(sessions, sessions, store, at("2026-11-20T08:00:00Z"), FakeLocalDataOwnerStore())()

        assertEquals(AuthStatus.LoggedIn, status)
    }

    @Test
    fun refreshVencido_pideLoginYBorraTodo() = runTest {
        val sessions = FakeSessionStore(testSession())
        val store = InMemoryTokenStore(tokens)

        val status = GetAuthStatusUseCase(sessions, sessions, store, at("2026-12-06T12:00:01Z"), FakeLocalDataOwnerStore())()

        assertEquals(AuthStatus.LoggedOut, status)
        assertNull(sessions.state.value)
        assertNull(store.tokens)
    }

    @Test
    fun sesionDeVersionAnteriorSinTokensCifrados_pideLogin() = runTest {
        val sessions = FakeSessionStore(testSession())

        val status = GetAuthStatusUseCase(sessions, sessions, InMemoryTokenStore(), at("2026-11-06T12:00:00Z"), FakeLocalDataOwnerStore())()

        assertEquals(AuthStatus.LoggedOut, status)
        assertNull(sessions.state.value)
    }

    @Test
    fun sinSesion_pideLogin() = runTest {
        val store = InMemoryTokenStore(tokens)

        val status = GetAuthStatusUseCase(FakeSessionStore(), FakeSessionStore(), store, at("2026-11-06T12:00:00Z"), FakeLocalDataOwnerStore())()

        assertEquals(AuthStatus.LoggedOut, status)
        assertNull(store.tokens)
    }

    @Test
    fun conductorSinLicencia_pideLicencia() = runTest {
        val sessions = FakeSessionStore(
            testSession(privileges = setOf("viaje.registrar"), requiresLicense = true),
        )

        val status = GetAuthStatusUseCase(sessions, sessions, InMemoryTokenStore(tokens), at("2026-11-06T12:00:00Z"), FakeLocalDataOwnerStore())()

        assertEquals(AuthStatus.NeedsLicense, status)
    }

    @Test
    fun sesionVencidaDeVersionAnterior_anotaDeQuienSonLosDatosAntesDeBorrarla() = runTest {
        val sessions = FakeSessionStore(testSession())
        val owners = FakeLocalDataOwnerStore()

        GetAuthStatusUseCase(sessions, sessions, InMemoryTokenStore(tokens), at("2026-12-06T12:00:01Z"), owners)()

        assertEquals(testSession().email, owners.owner?.email)
        assertEquals(testSession().userId, owners.owner?.userId)
    }
}
