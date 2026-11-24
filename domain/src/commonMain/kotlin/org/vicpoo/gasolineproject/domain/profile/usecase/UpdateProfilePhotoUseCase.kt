package org.vicpoo.gasolineproject.domain.profile.usecase

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.profile.ProfileRepository

/**
 * Foto de perfil: jpg, png o webp, máx. 5 MB (igual que la API).
 * Se recodifica antes de validar: pesa menos y pierde el EXIF (ubicación GPS de la galería).
 * Si no se puede leer se rechaza; nunca se sube la original.
 */
class UpdateProfilePhotoUseCase(
    private val repository: ProfileRepository,
    private val imageCompressor: ImageCompressor,
) {
    suspend operator fun invoke(original: PickedFile): AppResult<Unit> {
        if (original.mimeType.lowercase() !in ALLOWED_TYPES) {
            return AppResult.Failure(AppError.Validation(ValidationReason.INVALID_FILE_TYPE))
        }
        val file = imageCompressor.compress(original)
            ?: return AppResult.Failure(AppError.Validation(ValidationReason.INVALID_FILE_TYPE))
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
