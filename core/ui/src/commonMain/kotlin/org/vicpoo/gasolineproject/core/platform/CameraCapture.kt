package org.vicpoo.gasolineproject.core.platform

import androidx.compose.runtime.Composable

fun interface CameraLauncher {
    fun launch()
}

/**
 * Abre la cámara del teléfono y regresa la foto tomada como JPEG.
 * onResult recibe null si el usuario cancela.
 */
@Composable
expect fun rememberCameraCapture(onResult: (PickedFile?) -> Unit): CameraLauncher
