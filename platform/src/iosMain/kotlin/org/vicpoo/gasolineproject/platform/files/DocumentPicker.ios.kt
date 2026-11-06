package org.vicpoo.gasolineproject.platform.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.vicpoo.gasolineproject.domain.files.PickedFile

/**
 * TODO iOS: implementar con UIDocumentPickerViewController / PHPickerViewController.
 * Por ahora (prioridad Android) regresa null como si el usuario cancelara.
 */
@Composable
actual fun rememberDocumentPicker(
    mimeTypes: List<String>,
    onResult: (PickedFile?) -> Unit,
): DocumentPickerLauncher {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember { DocumentPickerLauncher { currentOnResult(null) } }
}
