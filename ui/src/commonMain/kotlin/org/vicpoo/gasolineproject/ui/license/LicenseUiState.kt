package org.vicpoo.gasolineproject.ui.license

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.files.PickedFile

data class LicenseUiState(
    val selectedFileName: String? = null,
    val isUploading: Boolean = false,
    val error: AppError? = null,
    /** Archivo pendiente para reintentar si falló la subida. */
    val pendingFile: PickedFile? = null,
    /** Hoja "Tomar foto / Elegir archivo". */
    val showSourceOptions: Boolean = false,
)
