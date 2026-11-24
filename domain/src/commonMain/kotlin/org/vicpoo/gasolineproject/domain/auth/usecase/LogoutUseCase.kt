package org.vicpoo.gasolineproject.domain.auth.usecase

import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository
import org.vicpoo.gasolineproject.domain.session.LocalDataOwnerStore
import org.vicpoo.gasolineproject.domain.session.LogoutAction

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
