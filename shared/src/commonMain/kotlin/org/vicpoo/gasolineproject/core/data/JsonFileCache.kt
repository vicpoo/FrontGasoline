package org.vicpoo.gasolineproject.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.core.platform.AppDirectories

/**
 * Caché offline genérica: guarda un valor serializable en un archivo JSON y lo expone como Flow.
 * Reutilizable por cualquier feature de solo lectura (historial de viajes, notificaciones, reportes...).
 */
class JsonFileCache<T : Any>(
    private val fileSystem: FileSystem,
    directories: AppDirectories,
    fileName: String,
    private val serializer: KSerializer<T>,
    private val json: Json,
    private val emptyValue: T,
) {
    private val dir = directories.filesDir.toPath() / "cache"
    private val file = dir / fileName
    private val temp = dir / "$fileName.tmp"
    private val mutex = Mutex()
    private val state = MutableStateFlow<T?>(null)

    val data: Flow<T> = state.onStart { load() }.filterNotNull()

    suspend fun current(): T {
        load()
        return state.value ?: emptyValue
    }

    suspend fun save(value: T) {
        mutex.withLock {
            withContext(Dispatchers.Default) {
                fileSystem.createDirectories(dir)
                fileSystem.write(temp) { writeUtf8(json.encodeToString(serializer, value)) }
                fileSystem.atomicMove(temp, file)
            }
            state.value = value
        }
    }

    suspend fun clear() {
        mutex.withLock {
            withContext(Dispatchers.Default) { runCatching { fileSystem.delete(file) } }
            state.value = emptyValue
        }
    }

    private suspend fun load() {
        mutex.withLock {
            if (state.value != null) return
            state.value = withContext(Dispatchers.Default) {
                runCatching {
                    if (!fileSystem.exists(file)) {
                        emptyValue
                    } else {
                        json.decodeFromString(serializer, fileSystem.read(file) { readUtf8() })
                    }
                }.getOrElse { emptyValue }
            }
        }
    }
}
