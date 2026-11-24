package org.vicpoo.gasolineproject.domain.files

/**
 * Foto guardada en el teléfono para mostrarla en pantalla (recibo o vehículo pendiente de subir).
 * Puede estar cifrada ([FileCipher]): el cargador de imágenes de la app (ImageCache en shared) la
 * descifra en memoria al mostrarla. Por eso NO se usa "file://ruta": Coil leería el archivo cifrado.
 */
data class LocalImage(val path: String)
