package org.vicpoo.gasolineproject.core.platform

/**
 * Reduce una foto antes de subirla (la API acepta máximo 5 MB).
 * Cada plataforma lo implementa; la lógica solo conoce esta interfaz.
 */
fun interface ImageCompressor {
    /** Regresa la imagen como JPEG más ligero, o la original si no es imagen o no se pudo. */
    suspend fun compress(file: PickedFile): PickedFile
}
