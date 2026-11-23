package org.vicpoo.gasolineproject.domain.files

/**
 * Reduce una foto antes de subirla (la API acepta máximo 5 MB).
 * Cada plataforma lo implementa; la lógica solo conoce esta interfaz.
 *
 * Privacidad: una foto SIEMPRE se recodifica, aunque la original ya pese poco. Así se quitan los
 * metadatos EXIF (incluida la ubicación GPS de donde se tomó). Nunca se sube la original.
 */
fun interface ImageCompressor {
    /**
     * - Imagen: la regresa recodificada como JPEG (sin EXIF).
     * - No es imagen (ej. un PDF): la regresa tal cual.
     * - Imagen que no se pudo leer o recodificar: null. Quien llama la rechaza (tipo de archivo inválido).
     */
    suspend fun compress(file: PickedFile): PickedFile?
}
