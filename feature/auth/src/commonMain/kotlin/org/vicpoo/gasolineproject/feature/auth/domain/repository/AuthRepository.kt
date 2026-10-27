package org.vicpoo.gasolineproject.feature.auth.domain.repository

import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.core.session.Session

/** Abstracción de autenticación: los casos de uso dependen de esto, no de Ktor ni DataStore. */
interface AuthRepository {
    suspend fun login(email: String, password: String): AppResult<Session>
    suspend fun uploadLicense(file: PickedFile): AppResult<Unit>

    /** Descarga los privilegios solo si cambiaron (offline-first: si no hay red se usan los guardados). */
    suspend fun refreshPrivileges(): AppResult<Unit>
    suspend fun logout()
}
