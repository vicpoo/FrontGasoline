package org.vicpoo.gasolineproject.domain.auth.repository

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.session.Session

/** Abstracción de autenticación: los casos de uso dependen de esto, no de Ktor ni DataStore. */
interface AuthRepository {
    /**
     * Inicia sesión. Con [clearLocalData] = true, si la API acepta la contraseña se borran los datos
     * que otra cuenta dejó en el teléfono ANTES de guardar la sesión nueva (si la contraseña es
     * incorrecta no se borra nada).
     */
    suspend fun login(email: String, password: String, clearLocalData: Boolean = false): AppResult<Session>
    suspend fun uploadLicense(file: PickedFile): AppResult<Unit>

    /** Descarga los privilegios solo si cambiaron (offline-first: si no hay red se usan los guardados). */
    suspend fun refreshPrivileges(): AppResult<Unit>
    suspend fun logout()
}
