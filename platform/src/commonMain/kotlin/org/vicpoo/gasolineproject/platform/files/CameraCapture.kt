package org.vicpoo.gasolineproject.platform.files

import androidx.compose.runtime.Composable
import org.vicpoo.gasolineproject.domain.files.PickedFile

fun interface CameraLauncher {
    fun launch()
}

/**
 * Abre la cámara del teléfono y regresa la foto tomada como JPEG.
 * onResult recibe null si el usuario cancela.
 */
@Composable
expect fun rememberCameraCapture(onResult: (PickedFile?) -> Unit): CameraLauncher
