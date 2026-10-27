package org.vicpoo.gasolineproject.core.platform

/** Archivo elegido por el usuario (foto o PDF). */
class PickedFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    val sizeBytes: Int get() = bytes.size
}
