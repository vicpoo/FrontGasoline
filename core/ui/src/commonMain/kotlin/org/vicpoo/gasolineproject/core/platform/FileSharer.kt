package org.vicpoo.gasolineproject.core.platform

import androidx.compose.runtime.Composable

/** Archivo generado (ej. un reporte PDF o Excel descargado de la API) para compartir o guardar. */
class SharedFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
)

/** Abre el menú de compartir del sistema con el archivo (WhatsApp, correo, Drive, "Guardar en Archivos"...). */
fun interface FileSharer {
    fun share(file: SharedFile)
}

@Composable
expect fun rememberFileSharer(): FileSharer
