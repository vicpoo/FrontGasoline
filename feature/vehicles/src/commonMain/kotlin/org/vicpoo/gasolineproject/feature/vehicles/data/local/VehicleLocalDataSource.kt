package org.vicpoo.gasolineproject.feature.vehicles.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.core.platform.AppDirectories
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.SyncState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Almacenamiento local de vehículos (fuente de verdad de la UI).
 * Es una interfaz: hoy se guarda en un archivo JSON; si después se migra a Room/SQLDelight
 * solo se cambia la implementación y nada más en la app se entera.
 */
interface VehicleLocalDataSource {
    val vehicles: Flow<List<VehicleEntity>>
    suspend fun getAll(): List<VehicleEntity>
    suspend fun get(localId: String): VehicleEntity?
    suspend fun upsert(entity: VehicleEntity)
    suspend fun remove(localId: String)

    /** Reemplaza los vehículos sincronizados con los de la API, respetando los pendientes. */
    suspend fun replaceSynced(remote: List<VehicleEntity>)

    suspend fun savePhoto(localId: String, bytes: ByteArray, extension: String): String
    suspend fun readPhoto(path: String): ByteArray?
    suspend fun deletePhoto(path: String)

    /** Borra todo lo guardado (al cerrar sesión). */
    suspend fun clearAll()
}

class JsonFileVehicleLocalDataSource(
    private val fileSystem: FileSystem,
    directories: AppDirectories,
    private val json: Json,
) : VehicleLocalDataSource {

    private val baseDir: Path = directories.filesDir.toPath() / "vehicles"
    private val dataFile: Path = baseDir / "vehicles.json"
    private val tempFile: Path = baseDir / "vehicles.json.tmp"
    private val photosDir: Path = baseDir / "photos"
    private val serializer = ListSerializer(VehicleEntity.serializer())

    private val mutex = Mutex()
    private val cache = MutableStateFlow<List<VehicleEntity>?>(null)

    override val vehicles: Flow<List<VehicleEntity>> =
        cache.onStart { ensureLoaded() }.filterNotNull()

    override suspend fun getAll(): List<VehicleEntity> = mutex.withLock { loadedLocked() }

    override suspend fun get(localId: String): VehicleEntity? = getAll().firstOrNull { it.localId == localId }

    override suspend fun upsert(entity: VehicleEntity) = edit { list ->
        val index = list.indexOfFirst { it.localId == entity.localId }
        if (index >= 0) list.toMutableList().also { it[index] = entity } else list + entity
    }

    override suspend fun remove(localId: String) = edit { list -> list.filterNot { it.localId == localId } }

    override suspend fun replaceSynced(remote: List<VehicleEntity>) = edit { current ->
        val pending = current.filter { it.syncState != SyncState.SYNCED }
        val pendingServerIds = pending.mapNotNull { it.serverId }.toSet()
        val currentByServerId = current.filter { it.serverId != null }.associateBy { it.serverId }

        val merged = remote
            .filter { it.serverId !in pendingServerIds }
            .map { incoming ->
                val previous = currentByServerId[incoming.serverId] ?: return@map incoming
                // Si la API ya tiene foto, la foto local ya no hace falta
                val keepLocalPhoto = incoming.photoUrl == null
                if (!keepLocalPhoto) previous.localPhotoPath?.let { deletePhotoFile(it) }
                incoming.copy(
                    localId = previous.localId,
                    localPhotoPath = if (keepLocalPhoto) previous.localPhotoPath else null,
                    localPhotoMime = if (keepLocalPhoto) previous.localPhotoMime else null,
                )
            }
        pending + merged
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun savePhoto(localId: String, bytes: ByteArray, extension: String): String =
        withContext(Dispatchers.Default) {
            fileSystem.createDirectories(photosDir)
            val path = photosDir / "$localId-${Uuid.random()}.$extension"
            fileSystem.write(path) { write(bytes) }
            path.toString()
        }

    override suspend fun readPhoto(path: String): ByteArray? = withContext(Dispatchers.Default) {
        runCatching { fileSystem.read(path.toPath()) { readByteArray() } }.getOrNull()
    }

    override suspend fun deletePhoto(path: String) = withContext(Dispatchers.Default) {
        deletePhotoFile(path)
    }

    override suspend fun clearAll() {
        mutex.withLock {
            withContext(Dispatchers.Default) {
                runCatching { fileSystem.deleteRecursively(baseDir) }
            }
            cache.value = emptyList()
        }
    }

    // ---------------- Internos ----------------

    private fun deletePhotoFile(path: String) {
        runCatching { fileSystem.delete(path.toPath()) }
    }

    private suspend fun ensureLoaded() {
        mutex.withLock { loadedLocked() }
    }

    /** Debe llamarse con el mutex tomado. */
    private suspend fun loadedLocked(): List<VehicleEntity> {
        cache.value?.let { return it }
        val loaded = withContext(Dispatchers.Default) { readFromDisk() }
        cache.value = loaded
        return loaded
    }

    private suspend fun edit(transform: (List<VehicleEntity>) -> List<VehicleEntity>) {
        mutex.withLock {
            val updated = transform(loadedLocked())
            withContext(Dispatchers.Default) { writeToDisk(updated) }
            cache.value = updated
        }
    }

    private fun readFromDisk(): List<VehicleEntity> = runCatching {
        if (!fileSystem.exists(dataFile)) return@runCatching emptyList()
        val text = fileSystem.read(dataFile) { readUtf8() }
        json.decodeFromString(serializer, text)
    }.getOrElse { emptyList() }

    /** Escritura atómica: primero a un temporal y luego se renombra. */
    private fun writeToDisk(list: List<VehicleEntity>) {
        fileSystem.createDirectories(baseDir)
        val text = json.encodeToString(serializer, list)
        fileSystem.write(tempFile) { writeUtf8(text) }
        fileSystem.atomicMove(tempFile, dataFile)
    }
}
