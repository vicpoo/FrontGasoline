package org.vicpoo.gasolineproject.core.platform

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Usa la app de cámara del sistema (TakePicturePreview): no necesita permiso CAMERA
 * ni FileProvider. Regresa una foto de tamaño reducido, suficiente para foto de perfil.
 */
@Composable
actual fun rememberCameraCapture(onResult: (PickedFile?) -> Unit): CameraLauncher {
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap == null) {
            currentOnResult(null)
        } else {
            scope.launch {
                val bytes = withContext(Dispatchers.Default) { bitmap.toJpeg() }
                currentOnResult(PickedFile(name = "foto_camara.jpg", mimeType = "image/jpeg", bytes = bytes))
            }
        }
    }

    return remember(launcher) { CameraLauncher { launcher.launch(null) } }
}

private fun Bitmap.toJpeg(): ByteArray = ByteArrayOutputStream().use { output ->
    compress(Bitmap.CompressFormat.JPEG, 90, output)
    output.toByteArray()
}
