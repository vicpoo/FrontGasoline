package org.vicpoo.gasolineproject.data.local

import okio.FileSystem
import okio.Path
import org.vicpoo.gasolineproject.domain.common.AppLog
import org.vicpoo.gasolineproject.domain.files.FileCipher

/**
 * Cifra en su lugar una foto guardada sin cifrar por la versión anterior: escribe el cifrado en un
 * temporal y lo renombra encima (atómico). El nombre no cambia, así que lo que apuntaba a la foto
 * (un viaje, una recarga, un vehículo) la sigue encontrando. Si ya estaba cifrada no hace nada.
 */
internal fun encryptInPlace(fileSystem: FileSystem, cipher: FileCipher, path: Path) {
    runCatching {
        val bytes = fileSystem.read(path) { readByteArray() }
        if (cipher.isEncrypted(bytes)) return
        val encrypted = cipher.encrypt(bytes)
        if (!cipher.isEncrypted(encrypted)) return // plataforma sin cifrado (iOS por ahora)
        val folder = checkNotNull(path.parent) { "La foto debe estar dentro de una carpeta" }
        val temp = folder / "${path.name}.tmp"
        fileSystem.write(temp) { write(encrypted) }
        fileSystem.atomicMove(temp, path)
    }.onFailure { AppLog.w("BaseLocal", "No se pudo cifrar una foto guardada", it) }
}
