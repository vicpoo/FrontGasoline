package org.vicpoo.gasolineproject.feature.auth.presentation.license

import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.platform.PickedFile

data class LicenseUiState(
    val selectedFileName: String? = null,
    val isUploading: Boolean = false,
    val error: AppError? = null,
    /** Archivo pendiente para reintentar si falló la subida. */
    val pendingFile: PickedFile? = null,
    /** Hoja "Tomar foto / Elegir archivo". */
    val showSourceOptions: Boolean = false,
)

sealed interface LicenseEvent {
    data class FilePicked(val file: PickedFile?) : LicenseEvent
    data object Retry : LicenseEvent
    data object UploadClicked : LicenseEvent
    data object SourceOptionsDismissed : LicenseEvent
}

sealed interface LicenseEffect {
    data object GoToHome : LicenseEffect
}
