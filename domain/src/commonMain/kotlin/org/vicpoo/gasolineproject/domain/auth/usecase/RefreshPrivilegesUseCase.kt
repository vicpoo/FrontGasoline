package org.vicpoo.gasolineproject.domain.auth.usecase

import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository
import org.vicpoo.gasolineproject.domain.common.AppResult

/** Actualiza los privilegios desde la API (si cambiaron). Sin red no pasa nada: se usan los guardados. */
class RefreshPrivilegesUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshPrivileges()
}
