package org.vicpoo.gasolineproject.domain.files

/**
 * Carpetas privadas de la app (las da cada plataforma).
 * - [filesDir]: datos offline que deben quedarse (cachés JSON, viajes, fotos pendientes de subir).
 * - [cacheDir]: lo que el sistema puede borrar si falta espacio (caché de imágenes, temporales).
 */
data class AppDirectories(
    val filesDir: String,
    val cacheDir: String = "${filesDir}_cache",
)
