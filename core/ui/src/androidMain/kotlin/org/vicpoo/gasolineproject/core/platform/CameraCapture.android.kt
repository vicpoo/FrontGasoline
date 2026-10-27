package org.vicpoo.gasolineproject.core.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Usa la app de cámara del sistema (TakePicture) con foto en resolución completa:
 * la cámara la guarda en un archivo temporal (FileProvider), se reduce a JPEG ≤ 1920 px
 * y se borra el temporal. No necesita el permiso CAMERA.
 * Requiere el <provider> "${applicationId}.fileprovider" del AndroidManifest de androidApp.
 */
@Composable
actual fun rememberCameraCapture(onResult: (PickedFile?) -> Unit): CameraLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingPath
        pendingPath = null
        if (!saved || path == null) {
            path?.let { File(it).delete() }
            currentOnResult(null)
        } else {
            scope.launch {
                val file = withContext(Dispatchers.IO) {
                    val temp = File(path)
                    val bytes = runCatching { AndroidImageCompressor.compressFile(temp) }.getOrNull()
                    temp.delete()
                    bytes?.let { PickedFile(name = "foto_camara.jpg", mimeType = "image/jpeg", bytes = it) }
                }
                currentOnResult(file)
            }
        }
    }

    return remember(launcher) {
        CameraLauncher {
            val temp = createTempPhoto(context)
            pendingPath = temp.absolutePath
            launcher.launch(uriFor(context, temp))
        }
    }
}

private fun createTempPhoto(context: Context): File {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    return File.createTempFile("foto_", ".jpg", dir)
}

private fun uriFor(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
