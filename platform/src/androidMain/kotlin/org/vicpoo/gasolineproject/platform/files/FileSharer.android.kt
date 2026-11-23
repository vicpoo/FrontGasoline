package org.vicpoo.gasolineproject.platform.files

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
 *
 * El nombre viene de la API (Content-Disposition): se limpia con [safeFileName] y además se verifica
 * que el archivo quede dentro de cache/reports, para que nunca se escriba en otra carpeta de la app.
 * El archivo no se borra aquí (el chooser no avisa cuándo terminó): lo limpia AndroidTemporaryFiles.
 */
@Composable
actual fun rememberFileSharer(): FileSharer {
    val context = LocalContext.current
    return remember(context) { FileSharer { file -> share(context, file) } }
}

private fun share(context: Context, file: SharedFile) {
    val folder = File(context.cacheDir, AndroidTemporaryFiles.REPORTS_DIR).apply { mkdirs() }
    val target = File(folder, safeFileName(file.name))
    // Segunda barrera: aunque un nombre raro pasara la limpieza, nunca se escribe fuera de cache/reports
    if (!target.isInside(folder)) return
    target.writeBytes(file.bytes)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = file.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, target.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, target.name).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

/** true si el archivo queda dentro de [folder] (comparando rutas reales, sin "..", ni enlaces). */
private fun File.isInside(folder: File): Boolean =
    canonicalPath.startsWith(folder.canonicalPath + File.separator)
