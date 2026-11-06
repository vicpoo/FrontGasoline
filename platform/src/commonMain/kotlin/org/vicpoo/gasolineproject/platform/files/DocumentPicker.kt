package org.vicpoo.gasolineproject.platform.files

import androidx.compose.runtime.Composable
import org.vicpoo.gasolineproject.domain.files.PickedFile

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
