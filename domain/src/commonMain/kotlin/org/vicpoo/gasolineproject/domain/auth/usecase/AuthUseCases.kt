package org.vicpoo.gasolineproject.domain.auth.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.session.LocalDataOwner
import org.vicpoo.gasolineproject.domain.session.LocalDataOwnerStore
import org.vicpoo.gasolineproject.domain.sync.CountPendingUseCase
import org.vicpoo.gasolineproject.domain.session.LogoutAction
import org.vicpoo.gasolineproject.domain.session.Session
import org.vicpoo.gasolineproject.domain.session.SessionReader
import org.vicpoo.gasolineproject.domain.session.SessionWriter
import org.vicpoo.gasolineproject.domain.session.TokenStore
import org.vicpoo.gasolineproject.domain.session.isSessionExpired
import org.vicpoo.gasolineproject.domain.license.mustUploadLicense
import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository

/** Resultado de iniciar sesión. */
sealed interface LoginResult {
    data class Success(val session: Session) : LoginResult

    data class Failure(val error: AppError) : LoginResult

    /**
     * En el teléfono hay [pendingCount] registros sin enviar de otra cuenta ([ownerEmail]).
     * Entrar con esta cuenta los borra: la pantalla pregunta y, si el usuario acepta, se vuelve a
     * llamar con discardOtherAccountData = true.
     */
    data class OtherAccountPendingData(val ownerEmail: String, val pendingCount: Int) : LoginResult
}

/**
 * Inicia sesión (la API pide email) cuidando los datos que hay en el teléfono:
 * - Misma cuenta que la última (ej. la sesión venció): se conserva todo y lo pendiente se envía.
 * - Otra cuenta sin nada pendiente: se borran las copias de la cuenta anterior al entrar.
 * - Otra cuenta CON registros sin enviar: primero se avisa ([LoginResult.OtherAccountPendingData]).
 */
class LoginUseCase(
    private val repository: AuthRepository,
    private val ownerStore: LocalDataOwnerStore,
    private val countPending: CountPendingUseCase,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        discardOtherAccountData: Boolean = false,
    ): LoginResult {
        val normalizedEmail = email.trim().lowercase()
        val owner = ownerStore.current()
        val otherAccount = owner != null && !owner.isSameAccount(normalizedEmail)
        if (otherAccount && !discardOtherAccountData) {
            val pending = countPending()
            if (pending > 0) return LoginResult.OtherAccountPendingData(owner!!.email, pending)
        }
        return when (val result = repository.login(normalizedEmail, password, clearLocalData = otherAccount)) {
            is AppResult.Failure -> LoginResult.Failure(result.error)
            is AppResult.Success -> {
                ownerStore.save(LocalDataOwner(userId = result.data.userId, email = result.data.email.ifBlank { normalizedEmail }))
                LoginResult.Success(result.data)
            }
        }
    }
}

/** Actualiza los privilegios desde la API (si cambiaron). Sin red no pasa nada: se usan los guardados. */
class RefreshPrivilegesUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshPrivileges()
}

/** Cierra sesión: revoca el refresh token en la API (si hay red) y borra la sesión y los tokens locales. */
class LogoutUseCase(
    private val repository: AuthRepository,
    private val ownerStore: LocalDataOwnerStore,
) : LogoutAction {
    override suspend operator fun invoke() {
        repository.logout()
        // Los datos del teléfono ya se borraron: no son de nadie
        ownerStore.clear()
    }
}

/** true mientras haya sesión; cambia a false al cerrar sesión o si la API rechazó el refresh token. */
class ObserveIsLoggedInUseCase(private val sessionReader: SessionReader) {
    operator fun invoke(): Flow<Boolean> = sessionReader.session.map { it != null }.distinctUntilChanged()
}

/** En qué estado está el usuario al abrir la app. */
enum class AuthStatus { LoggedOut, NeedsLicense, LoggedIn }

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
