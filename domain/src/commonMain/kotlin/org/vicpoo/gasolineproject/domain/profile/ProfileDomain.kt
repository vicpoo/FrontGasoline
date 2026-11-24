package org.vicpoo.gasolineproject.domain.profile

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.files.PickedFile

/** Perfil del usuario (sale de la sesión guardada: se ve aunque no haya red). */
data class Profile(
    val name: String,
    val email: String,
    val photoUrl: String?,
    val roles: List<String>,
    /** false = usuario sin empleado (ej. Administrador): no puede editar nombre, foto ni licencia. */
    val hasEmployee: Boolean,
    /** Tiene viaje.registrar: solo a él se le muestra y se le pide la licencia. */
    val drives: Boolean = false,
    val hasLicense: Boolean = false,
)

interface ProfileRepository {
    fun observeProfile(): Flow<Profile?>

    /** Trae nombre y foto actualizados de la API (si no hay red se queda lo guardado). */
    suspend fun refresh(): AppResult<Unit>
    suspend fun updateName(name: String): AppResult<Unit>
    suspend fun updatePhoto(file: PickedFile): AppResult<Unit>
}

