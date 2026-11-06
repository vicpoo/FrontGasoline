package org.vicpoo.gasolineproject.platform.files

import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile

/** iOS: pendiente (UIImage + jpegData). Por ahora se sube la imagen tal cual. */
class IosImageCompressor : ImageCompressor {
    override suspend fun compress(file: PickedFile): PickedFile = file
}
