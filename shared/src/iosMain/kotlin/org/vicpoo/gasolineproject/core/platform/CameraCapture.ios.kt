package org.vicpoo.gasolineproject.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * TODO iOS: implementar con UIImagePickerController (sourceType = camera).
 * Por ahora (prioridad Android) regresa null como si el usuario cancelara.
 */
@Composable
actual fun rememberCameraCapture(onResult: (PickedFile?) -> Unit): CameraLauncher {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember { CameraLauncher { currentOnResult(null) } }
}
