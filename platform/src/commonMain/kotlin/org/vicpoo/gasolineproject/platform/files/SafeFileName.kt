package org.vicpoo.gasolineproject.platform.files

/**
 * Nombre seguro para guardar en disco un archivo cuyo nombre viene de fuera (ej. el header
 * Content-Disposition de un reporte descargado de la API).
 *
 * La API va por HTTP sin TLS: un tercero en la red podría mandar un nombre como
 * "../../files/datastore/sesion.preferences_pb" para escribir fuera de la carpeta de reportes
 * (path traversal). Aquí:
 * - se queda solo el último segmento (sin carpetas, con "/" o "\"),
 * - se quitan caracteres de control y los que no valen en un nombre de archivo,
 * - se quitan los puntos del inicio (".." y archivos ocultos) y los puntos o espacios del final,
 * - se limita el largo conservando la extensión,
 * - si no queda nada, se usa [fallback].
 */
fun safeFileName(raw: String, fallback: String = DEFAULT_FILE_NAME): String {
    val lastSegment = raw.substringAfterLast('/').substringAfterLast('\\')
    val cleaned = lastSegment
        .filterNot { it.isISOControl() || it in FORBIDDEN_CHARS }
        .trim()
        .trimStart('.')
        .trimEnd('.', ' ')
    if (cleaned.isEmpty()) return fallback
    return if (cleaned.length <= MAX_FILE_NAME_LENGTH) cleaned else shorten(cleaned)
}

/** Nombre que se usa si el que llegó no sirve. */
const val DEFAULT_FILE_NAME = "archivo"

/** Largo máximo del nombre (los sistemas de archivos aceptan 255 bytes; se deja margen para UTF-8). */
const val MAX_FILE_NAME_LENGTH = 120

private const val FORBIDDEN_CHARS = "<>:\"|?*"
private const val MAX_EXTENSION_LENGTH = 10

private fun shorten(name: String): String {
    val extension = name.substringAfterLast('.', missingDelimiterValue = "")
    if (extension.isEmpty() || extension.length > MAX_EXTENSION_LENGTH) return name.take(MAX_FILE_NAME_LENGTH)
    val base = name.dropLast(extension.length + 1).take(MAX_FILE_NAME_LENGTH - extension.length - 1).trimEnd('.', ' ')
    return "$base.$extension"
}
