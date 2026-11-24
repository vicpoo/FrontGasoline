package org.vicpoo.gasolineproject.domain.auth.usecase

import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.license.mustUploadLicense
import org.vicpoo.gasolineproject.domain.session.LocalDataOwner
import org.vicpoo.gasolineproject.domain.session.LocalDataOwnerStore
import org.vicpoo.gasolineproject.domain.session.SessionReader
import org.vicpoo.gasolineproject.domain.session.SessionWriter
import org.vicpoo.gasolineproject.domain.session.TokenStore
import org.vicpoo.gasolineproject.domain.session.isSessionExpired

/**
 * Decide por dónde entra la app. Funciona SIN red (offline-first):
 * - Que el JWT de 15 min haya vencido NO importa: se renueva solo en la primera petición con red.
 * - Lo que cuenta es el refresh token: si venció (30 días sin abrir la app, o el tope de 90) hay que
 *   iniciar sesión. Si la API lo revocó, se sabrá en la primera petición con red (401 -> login).
 * - Sin tokens guardados (por ejemplo, la sesión de una versión anterior de la app, que guardaba el token
 *   sin cifrar) se borra todo y se pide iniciar sesión.
 */
class GetAuthStatusUseCase(
    private val sessionReader: SessionReader,
    private val sessionWriter: SessionWriter,
    private val tokenStore: TokenStore,
    private val clock: AppClock,
    private val ownerStore: LocalDataOwnerStore,
) {
    suspend operator fun invoke(): AuthStatus {
        val session = sessionReader.current()
        val tokens = tokenStore.read()

        // Instalaciones de antes de LocalDataOwner: los datos del teléfono son de la última sesión.
        // Se anota ANTES de borrar una sesión vencida, para reconocer si después entra otra cuenta.
        if (session != null && ownerStore.current() == null) {
            ownerStore.save(LocalDataOwner(userId = session.userId, email = session.email))
        }

        if (session == null || tokens == null || tokens.isSessionExpired(clock.nowMillis())) {
            if (session != null) sessionWriter.clear()
            if (tokens != null) tokenStore.clear()
            return AuthStatus.LoggedOut
        }
        // Solo a quien maneja (viaje.registrar) se le pide la licencia
        return if (session.mustUploadLicense) AuthStatus.NeedsLicense else AuthStatus.LoggedIn
    }
}
