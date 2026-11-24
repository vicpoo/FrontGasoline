package org.vicpoo.gasolineproject.domain.license.usecase

import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile

/** Sube la licencia de conducir validando tipo y tamaño (mismas reglas que la API: jpg/png/webp/pdf, 5 MB). */
class UploadLicenseUseCase(
    private val repository: AuthRepository,
    private val imageCompressor: ImageCompressor,
) {

    suspend operator fun invoke(original: PickedFile): AppResult<Unit> {
        // Las fotos se recodifican antes de validar: pesan menos de 5 MB y pierden el EXIF (ubicación).
        // Si la foto no se puede leer se rechaza en vez de subir la original. Los PDF no se tocan.
        val file = if (original.mimeType.startsWith("image/")) {
            imageCompressor.compress(original)
                ?: return AppResult.Failure(AppError.Validation(ValidationReason.INVALID_FILE_TYPE))
        } else {
            original
        }
        if (file.mimeType.lowercase() !in ALLOWED_TYPES) {
            return AppResult.Failure(AppError.Validation(ValidationReason.INVALID_FILE_TYPE))
        }
        if (file.sizeBytes > MAX_SIZE_BYTES) {
            return AppResult.Failure(AppError.Validation(ValidationReason.FILE_TOO_LARGE))
        }
        return repository.uploadLicense(file)
    }

    companion object {
        private val ALLOWED_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp", "application/pdf")
        val ALLOWED_MIME_TYPES = listOf("image/jpeg", "image/png", "image/webp", "application/pdf")
        const val MAX_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB
    }
}
