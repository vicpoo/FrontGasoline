package org.vicpoo.gasolineproject.feature.profile.domain

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.platform.PickedFile

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

class ObserveProfileUseCase(private val repository: ProfileRepository) {
    operator fun invoke(): Flow<Profile?> = repository.observeProfile()
}

class RefreshProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}

/** Nombre completo: obligatorio, máx. 150 caracteres (igual que la API). */
class UpdateNameUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(name: String): AppResult<Unit> {
        val clean = name.trim()
        if (clean.isEmpty()) return AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED))
        if (clean.length > MAX_NAME) return AppResult.Failure(AppError.Validation(ValidationReason.OUT_OF_RANGE))
        return repository.updateName(clean)
    }

    companion object {
        const val MAX_NAME = 150
    }
}

/** Foto de perfil: jpg, png o webp, máx. 5 MB (igual que la API). */
class UpdateProfilePhotoUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(file: PickedFile): AppResult<Unit> {
        if (file.mimeType.lowercase() !in ALLOWED_TYPES) {
            return AppResult.Failure(AppError.Validation(ValidationReason.INVALID_FILE_TYPE))
        }
        if (file.sizeBytes > MAX_BYTES) {
            return AppResult.Failure(AppError.Validation(ValidationReason.FILE_TOO_LARGE))
        }
        return repository.updatePhoto(file)
    }

    companion object {
        private val ALLOWED_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
        val PICKER_MIME_TYPES = listOf("image/jpeg", "image/png", "image/webp")
        const val MAX_BYTES = 5 * 1024 * 1024
    }
}
