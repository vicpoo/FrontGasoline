package org.vicpoo.gasolineproject.data.local

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
import org.vicpoo.gasolineproject.data.database.LocalDatabase
import org.vicpoo.gasolineproject.domain.common.AppLog
import org.vicpoo.gasolineproject.domain.files.AppDirectories

/**
 * Caché offline genérica: guarda un valor serializable como un documento JSON dentro de la base local
 * cifrada (tabla CacheDocumentEntity) y lo expone como Flow. La usan el registro de viajes, la cola de
 * solicitudes, las reservas, el historial, los reportes, los carros asignados, los centros de costo y
 * la bitácora de envíos.
 *
 * Fase 6F: antes cada caché era un archivo cache/[fileName] sin cifrar. La primera vez que se lee,
 * si ese archivo existe, su contenido se pasa a la base y el archivo se borra (ver [importLegacyFile]).
 */
class LocalDocumentCache<T : Any>(
    private val database: LocalDatabase,
    private val fileSystem: FileSystem,
    directories: AppDirectories,
    /** Nombre del archivo viejo ("trip_recording.json"); sin ".json" es el nombre del documento. */
    fileName: String,
    private val serializer: KSerializer<T>,
    private val json: Json,
    private val emptyValue: T,
) {
    private val name = fileName.removeSuffix(".json")
    private val legacyDir = directories.filesDir.toPath() / "cache"
    private val legacyFile = legacyDir / fileName
    private val legacyTemp = legacyDir / "$fileName.tmp"
    private val mutex = Mutex()
    private val state = MutableStateFlow<T?>(null)

    val data: Flow<T> = state.onStart { load() }.filterNotNull()

    suspend fun current(): T {
        load()
        return state.value ?: emptyValue
    }

    suspend fun save(value: T) {
        mutex.withLock {
            val text = json.encodeToString(serializer, value)
            database.use { cacheDocumentQueries.put(name, text) }
            state.value = value
        }
    }

    suspend fun clear() {
        mutex.withLock {
            database.use { cacheDocumentQueries.remove(name) }
            deleteLegacyFiles()
            state.value = emptyValue
        }
    }

    private suspend fun load() {
        mutex.withLock {
            if (state.value != null) return
            importLegacyFile()
            val stored = database.use { cacheDocumentQueries.get(name).executeAsOneOrNull() }
            state.value = stored?.let { decode(it) } ?: emptyValue
        }
    }

    /**
     * Pasa el archivo de la versión anterior a la base. Primero se guarda en la base y después se borra
     * el archivo: si la app se cierra a la mitad, la siguiente vez el documento ya existe, no se pisa y
     * solo se borra el archivo. Si el archivo estaba dañado se deja de usar (igual que antes: caché vacío).
     */
    private suspend fun importLegacyFile() {
        val valid = withContext(Dispatchers.Default) {
            if (!fileSystem.exists(legacyFile)) {
                if (fileSystem.exists(legacyTemp)) deleteLegacyFiles()
                return@withContext NO_FILE
            }
            runCatching { fileSystem.read(legacyFile) { readUtf8() } }.getOrNull()?.takeIf { decode(it) != null }
        }
        if (valid == NO_FILE) return
        database.use {
            if (valid != null && cacheDocumentQueries.get(name).executeAsOneOrNull() == null) {
                cacheDocumentQueries.put(name, valid)
            }
        }
        if (valid == null) AppLog.w(LOG_TAG, "El archivo $name de la versión anterior estaba dañado; se descarta")
        withContext(Dispatchers.Default) { deleteLegacyFiles() }
    }

    private fun decode(text: String): T? = runCatching { json.decodeFromString(serializer, text) }.getOrNull()

    private fun deleteLegacyFiles() {
        runCatching { fileSystem.delete(legacyFile) }
        // Si una escritura de la versión anterior quedó a medias, también queda el temporal con datos
        runCatching { fileSystem.delete(legacyTemp) }
    }

    private companion object {
        const val LOG_TAG = "BaseLocal"

        /** Marca "no había archivo viejo" (distinta de null = archivo dañado). */
        const val NO_FILE = "\u0000sin-archivo"
    }
}
