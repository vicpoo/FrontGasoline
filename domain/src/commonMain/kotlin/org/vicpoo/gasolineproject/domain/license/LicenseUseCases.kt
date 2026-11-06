package org.vicpoo.gasolineproject.domain.license

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.session.SessionReader
import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository

/** Sube la licencia de conducir validando tipo y tamaño (mismas reglas que la API: jpg/png/webp/pdf, 5 MB). */
class UploadLicenseUseCase(
    private val repository: AuthRepository,
    private val imageCompressor: ImageCompressor,
) {

    suspend operator fun invoke(original: PickedFile): AppResult<Unit> {
        // Las fotos del teléfono suelen pesar más de 5 MB: se reducen antes de validar (los PDF no se tocan)
        val file = if (original.mimeType.startsWith("image/")) imageCompressor.compress(original) else original
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

/** true si el usuario maneja (viaje.registrar) y aún no sube su licencia (cambia si le dan el privilegio). */
class ObserveMustUploadLicenseUseCase(private val sessionReader: SessionReader) {
    operator fun invoke(): Flow<Boolean> = sessionReader.session.map { it?.mustUploadLicense == true }.distinctUntilChanged()
}
