package org.vicpoo.gasolineproject.core.platform

import androidx.compose.runtime.Composable

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
