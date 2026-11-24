package org.vicpoo.gasolineproject.image

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.domain.files.FileCipher
import org.vicpoo.gasolineproject.domain.files.LocalImage

/**
 * Muestra con Coil una foto guardada en el teléfono ([LocalImage]): la lee, la descifra en memoria
 * con [FileCipher] y se la pasa a Coil. La foto descifrada nunca se escribe en disco (Coil no guarda
 * en su caché de disco lo que viene de un archivo local); en memoria queda en la caché de Coil, que se
 * borra al cerrar sesión. Las fotos viejas sin cifrar también se muestran (decrypt las regresa igual).
 */
class LocalImageFetcher(
    private val image: LocalImage,
    private val options: Options,
    private val fileSystem: FileSystem,
    private val cipher: FileCipher,
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.Default) {
        val stored = fileSystem.read(image.path.toPath()) { readByteArray() }
        val bytes = cipher.decrypt(stored)
        SourceFetchResult(
            source = ImageSource(source = Buffer().write(bytes), fileSystem = options.fileSystem),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val fileSystem: FileSystem, private val cipher: FileCipher) : Fetcher.Factory<LocalImage> {
        override fun create(data: LocalImage, options: Options, imageLoader: ImageLoader): Fetcher =
            LocalImageFetcher(data, options, fileSystem, cipher)
    }
}
