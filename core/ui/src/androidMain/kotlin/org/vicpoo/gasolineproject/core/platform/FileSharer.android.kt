package org.vicpoo.gasolineproject.core.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android: guarda el archivo en cache/reports y lo comparte con el FileProvider de la app
 * (res/xml/file_paths.xml de androidApp debe tener <cache-path name="reports" path="reports/" />).
 */
@Composable
actual fun rememberFileSharer(): FileSharer {
    val context = LocalContext.current
    return remember(context) { FileSharer { file -> share(context, file) } }
}

private fun share(context: Context, file: SharedFile) {
    val folder = File(context.cacheDir, "reports").apply { mkdirs() }
    val target = File(folder, file.name)
    target.writeBytes(file.bytes)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = file.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, file.name).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
