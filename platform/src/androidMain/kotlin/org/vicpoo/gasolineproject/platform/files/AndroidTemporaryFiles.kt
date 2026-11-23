package org.vicpoo.gasolineproject.platform.files

import java.io.File

/**
 * Archivos temporales de la app en cacheDir:
 * - camera/: la foto que guarda la app de cámara antes de reducirla (se borra al leerla).
 * - reports/: el PDF / Excel que se comparte (el chooser no avisa cuándo terminó la otra app).
 *
 * Nada de esto debe quedarse en el teléfono:
 * - Al abrir la app se borran los que tengan más de [MAX_AGE_MILLIS]. No se borra todo: si Android
 *   cerró la app mientras el usuario estaba en la cámara, la foto recién tomada sigue ahí y se lee al
 *   regresar; y la app con la que se compartió un reporte puede seguir leyéndolo un rato.
 * - Al cerrar sesión se borra todo (LogoutListener en platformModule).
 */
class AndroidTemporaryFiles(
    private val cacheDir: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val cameraDir: File get() = File(cacheDir, CAMERA_DIR)
    val reportsDir: File get() = File(cacheDir, REPORTS_DIR)

    /** Al abrir la app: solo lo viejo. */
    fun clearStale() {
        val limit = now() - MAX_AGE_MILLIS
        listOf(cameraDir, reportsDir).forEach { dir ->
            dir.listFiles()?.filter { it.lastModified() < limit }?.forEach { it.deleteRecursively() }
        }
    }

    /** Al cerrar sesión: todo. */
    fun clearAll() {
        cameraDir.deleteRecursively()
        reportsDir.deleteRecursively()
    }

    companion object {
        /** Deben coincidir con res/xml/file_paths.xml de androidApp. */
        const val CAMERA_DIR = "camera"
        const val REPORTS_DIR = "reports"
        const val MAX_AGE_MILLIS = 60 * 60 * 1000L
    }
}
