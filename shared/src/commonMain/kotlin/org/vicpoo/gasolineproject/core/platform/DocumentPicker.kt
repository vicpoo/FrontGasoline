package org.vicpoo.gasolineproject.core.platform

import androidx.compose.runtime.Composable

/** Archivo elegido por el usuario (foto o PDF). */
class PickedFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    val sizeBytes: Int get() = bytes.size
}

fun interface DocumentPickerLauncher {
    fun launch()
}

/**
 * Selector de archivos nativo de cada plataforma.
 * onResult recibe null si el usuario cancela.
 */
@Composable
expect fun rememberDocumentPicker(
    mimeTypes: List<String>,
    onResult: (PickedFile?) -> Unit,
): DocumentPickerLauncher
