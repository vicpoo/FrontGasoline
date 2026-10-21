package org.vicpoo.gasolineproject.feature.auth.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.platform.ImageCompressor
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.core.session.Session
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.core.session.SessionWriter
import org.vicpoo.gasolineproject.core.session.mustUploadLicense
import org.vicpoo.gasolineproject.feature.auth.domain.repository.AuthRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Inicia sesión (la API pide email). */
class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AppResult<Session> =
        repository.login(email.trim().lowercase(), password)
}

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

/** Actualiza los privilegios desde la API (si cambiaron). Sin red no pasa nada: se usan los guardados. */
class RefreshPrivilegesUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshPrivileges()
}

/** Cierra sesión (borra la sesión local). */
class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.logout()
}

/** true mientras haya sesión; cambia a false al cerrar sesión o si el token venció (401). */
class ObserveIsLoggedInUseCase(private val sessionReader: SessionReader) {
    operator fun invoke(): Flow<Boolean> = sessionReader.session.map { it != null }.distinctUntilChanged()
}

/** true si el usuario maneja (viaje.registrar) y aún no sube su licencia (cambia si le dan el privilegio). */
class ObserveMustUploadLicenseUseCase(private val sessionReader: SessionReader) {
    operator fun invoke(): Flow<Boolean> = sessionReader.session.map { it?.mustUploadLicense == true }.distinctUntilChanged()
}

/** En qué estado está el usuario al abrir la app. */
enum class AuthStatus { LoggedOut, NeedsLicense, LoggedIn }

class GetAuthStatusUseCase(
    private val sessionReader: SessionReader,
    private val sessionWriter: SessionWriter,
) {
    suspend operator fun invoke(): AuthStatus {
        val session = sessionReader.current() ?: return AuthStatus.LoggedOut
        if (session.isExpired()) {
            sessionWriter.clear()
            return AuthStatus.LoggedOut
        }
        // Solo a quien maneja (viaje.registrar) se le pide la licencia
        return if (session.mustUploadLicense) AuthStatus.NeedsLicense else AuthStatus.LoggedIn
    }
}

/** true si el JWT ya venció (si la fecha no se puede leer, se asume vigente y la API decidirá con 401). */
@OptIn(ExperimentalTime::class)
private fun Session.isExpired(): Boolean {
    val raw = expiresAtUtc ?: return false
    val normalized = if (raw.endsWith("Z") || raw.contains('+')) raw else "${raw}Z"
    val expiresAt = runCatching { Instant.parse(normalized) }.getOrNull() ?: return false
    return Clock.System.now() >= expiresAt
}
