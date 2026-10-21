package org.vicpoo.gasolineproject.core.platform

/** iOS: pendiente (UIImage + jpegData). Por ahora se sube la imagen tal cual. */
class IosImageCompressor : ImageCompressor {
    override suspend fun compress(file: PickedFile): PickedFile = file
}
