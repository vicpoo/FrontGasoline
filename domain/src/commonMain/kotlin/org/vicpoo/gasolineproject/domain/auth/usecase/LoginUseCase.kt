package org.vicpoo.gasolineproject.domain.auth.usecase

import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.session.LocalDataOwner
import org.vicpoo.gasolineproject.domain.session.LocalDataOwnerStore
import org.vicpoo.gasolineproject.domain.sync.usecase.CountLostOnLogoutUseCase

/**
 * Inicia sesión (la API pide email) cuidando los datos que hay en el teléfono:
 * - Misma cuenta que la última (ej. la sesión venció): se conserva todo y lo pendiente se envía.
 * - Otra cuenta sin nada pendiente: se borran las copias de la cuenta anterior al entrar.
 * - Otra cuenta CON registros sin enviar: primero se avisa ([LoginResult.OtherAccountPendingData]).
 * - Los viajes de la otra cuenta (en curso o sin enviar) NO se borran ni se cuentan: se quedan
 *   guardados aparte y solo los ve, envía y retoma su dueño cuando vuelva a entrar.
 */
class LoginUseCase(
    private val repository: AuthRepository,
    private val ownerStore: LocalDataOwnerStore,
    private val countPending: CountLostOnLogoutUseCase,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        discardOtherAccountData: Boolean = false,
    ): LoginResult {
        val normalizedEmail = email.trim().lowercase()
        // Dueño de los datos del teléfono, solo si es OTRA cuenta
        val otherOwner = ownerStore.current()?.takeIf { !it.isSameAccount(normalizedEmail) }
        val otherAccount = otherOwner != null
        if (otherOwner != null && !discardOtherAccountData) {
            val pending = countPending()
            if (pending > 0) return LoginResult.OtherAccountPendingData(otherOwner.email, pending)
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
