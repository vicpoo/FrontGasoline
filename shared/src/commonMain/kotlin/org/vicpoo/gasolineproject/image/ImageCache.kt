package org.vicpoo.gasolineproject.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.files.AppDirectories
import org.vicpoo.gasolineproject.domain.files.FileCipher
import org.vicpoo.gasolineproject.domain.session.LogoutListener

/**
 * Cargador de imágenes de la app (Coil) con la caché configurada a propósito, en vez de los valores
 * por defecto:
 * - Disco: [AppDirectories.cacheDir]/image_cache (interno, privado de la app), máximo [MAX_DISK_BYTES].
 * - Memoria: la de Coil por defecto (un porcentaje de la memoria de la app).
 *
 * Ahí quedan las fotos de vehículos, operadores y perfil descargadas del servidor: al cerrar sesión
 * se borran las dos cachés ([clear], registrado como LogoutListener en [imageModule]).
 *
 * Las fotos guardadas en el teléfono (LocalImage, cifradas) las lee [LocalImageFetcher].
 */
class ImageCache(
    directories: AppDirectories,
    private val fileSystem: FileSystem,
    private val fileCipher: FileCipher,
) {

    val directory: Path = directories.cacheDir.toPath() / DIRECTORY_NAME

    /** El que está usando la app (null si en este proceso aún no se abrió ninguna pantalla). */
    private var loader: ImageLoader? = null

    fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
                add(LocalImageFetcher.Factory(fileSystem, fileCipher))
            }
            .diskCache { newDiskCache() }
            .crossfade(true)
            .build()
            .also { loader = it }

    /** Borra las fotos guardadas (memoria y disco). */
    fun clear() {
        val current = loader
        if (current != null) {
            current.memoryCache?.clear()
            current.diskCache?.clear()
        } else {
            // Sin pantallas en este proceso (ej. la sesión terminó en segundo plano): se limpia la carpeta igual
            newDiskCache().clear()
        }
    }

    private fun newDiskCache(): DiskCache =
        DiskCache.Builder()
            .directory(directory)
            .maxSizeBytes(MAX_DISK_BYTES)
            .build()

    companion object {
        const val DIRECTORY_NAME = "image_cache"

        /** 100 MB: de sobra para las fotos de la flota y las teselas del mapa del historial. */
        const val MAX_DISK_BYTES = 100L * 1024 * 1024
    }
}

/** Caché de imágenes y su limpieza al cerrar sesión. */
val imageModule = module {
    single { ImageCache(directories = get(), fileSystem = get(), fileCipher = get()) }
    single<LogoutListener>(named("imageCache")) {
        val cache = get<ImageCache>()
        LogoutListener { withContext(Dispatchers.Default) { cache.clear() } }
    }
}
