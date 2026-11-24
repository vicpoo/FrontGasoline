package org.vicpoo.gasolineproject.data.vehicles.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.data.database.LocalDatabase
import org.vicpoo.gasolineproject.data.local.encryptInPlace
import org.vicpoo.gasolineproject.database.AppDatabase
import org.vicpoo.gasolineproject.domain.common.AppLog
import org.vicpoo.gasolineproject.domain.files.AppDirectories
import org.vicpoo.gasolineproject.domain.files.FileCipher
import org.vicpoo.gasolineproject.domain.vehicles.model.SyncState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Almacenamiento local de vehículos (fuente de verdad de la UI). Es una interfaz: la implementación
 * se puede cambiar sin que nada más en la app se entere (fase 6: pasó de un archivo JSON a SQLDelight).
 */
interface VehicleLocalDataSource {
    /** Lo que ve la UI: todo menos los borrados sin conexión que esperan mandarse (filtro en SQL). */
    fun observeVisible(): Flow<List<VehicleEntity>>
    fun observeVisible(localId: String): Flow<VehicleEntity?>

    /** Cuántos tienen cambios sin mandar a la API (conteo en SQL). */
    fun observePendingCount(): Flow<Int>

    suspend fun getAll(): List<VehicleEntity>
    suspend fun get(localId: String): VehicleEntity?
    suspend fun upsert(entity: VehicleEntity)
    suspend fun remove(localId: String)

    /** Reemplaza los vehículos sincronizados con los de la API, respetando los pendientes. */
    suspend fun replaceSynced(remote: List<VehicleEntity>)

    suspend fun savePhoto(localId: String, bytes: ByteArray, extension: String): String

    /** La foto ya descifrada; null si no existe o no se puede descifrar. */
    suspend fun readPhoto(path: String): ByteArray?
    suspend fun deletePhoto(path: String)

    /** Borra todo lo guardado (al cerrar sesión). */
    suspend fun clearAll()
}

/**
 * Vehículos en la tabla VehicleEntity de la base local cifrada; fotos pendientes en vehicles/photos,
 * cifradas con [FileCipher].
 *
 * Fase 6F: la primera vez que se usa, si existe vehicles/vehicles.json de la versión anterior, sus
 * vehículos se pasan a la tabla y el archivo se borra; [encryptLegacyPhotos] cifra las fotos viejas.
 */
class SqlVehicleLocalDataSource(
    private val database: LocalDatabase,
    private val fileSystem: FileSystem,
    directories: AppDirectories,
    private val json: Json,
    private val fileCipher: FileCipher,
) : VehicleLocalDataSource {

    private val baseDir: Path = directories.filesDir.toPath() / "vehicles"
    private val legacyFile: Path = baseDir / "vehicles.json"
    private val legacyTemp: Path = baseDir / "vehicles.json.tmp"
    private val photosDir: Path = baseDir / "photos"

    /** Protege los leer-modificar-guardar (replaceSynced) y la importación del archivo viejo. */
    private val mutex = Mutex()
    private var legacyChecked = false

    override fun observeVisible(): Flow<List<VehicleEntity>> = observe { vehicleQueries.selectVisible() }
        .map { rows -> rows.mapNotNull { decode(it) } }

    override fun observeVisible(localId: String): Flow<VehicleEntity?> = flow {
        val db = ready()
        emitAll(db.vehicleQueries.selectVisibleById(localId).asFlow().mapToOneOrNull(Dispatchers.Default))
    }.map { row -> row?.let { decode(it) } }

    override fun observePendingCount(): Flow<Int> = flow {
        val db = ready()
        emitAll(db.vehicleQueries.countPending().asFlow().mapToOne(Dispatchers.Default))
    }.map { it.toInt() }

    override suspend fun getAll(): List<VehicleEntity> {
        ready()
        return database.use { vehicleQueries.selectAll().executeAsList() }.mapNotNull { decode(it) }
    }

    override suspend fun get(localId: String): VehicleEntity? {
        ready()
        return database.use { vehicleQueries.selectById(localId).executeAsOneOrNull() }?.let { decode(it) }
    }

    override suspend fun upsert(entity: VehicleEntity) {
        ready()
        mutex.withLock { database.use { insert(entity) } }
    }

    override suspend fun remove(localId: String) {
        ready()
        mutex.withLock { database.use { vehicleQueries.remove(localId) } }
    }

    override suspend fun replaceSynced(remote: List<VehicleEntity>) {
        ready()
        mutex.withLock {
            val photosToDelete = mutableListOf<String>()
            database.use {
                vehicleQueries.transaction {
                    val current = vehicleQueries.selectAll().executeAsList().mapNotNull { decode(it) }
                    val pending = current.filter { it.syncState != SyncState.SYNCED }
                    val pendingServerIds = pending.mapNotNull { it.serverId }.toSet()
                    val currentByServerId = current.filter { it.serverId != null }.associateBy { it.serverId }

                    val merged = remote
                        .filter { it.serverId !in pendingServerIds }
                        .map { incoming ->
                            val previous = currentByServerId[incoming.serverId] ?: return@map incoming
                            // Si la API ya tiene foto, la foto local ya no hace falta
                            val keepLocalPhoto = incoming.photoUrl == null
                            if (!keepLocalPhoto) previous.localPhotoPath?.let { photosToDelete += it }
                            incoming.copy(
                                localId = previous.localId,
                                localPhotoPath = if (keepLocalPhoto) previous.localPhotoPath else null,
                                localPhotoMime = if (keepLocalPhoto) previous.localPhotoMime else null,
                            )
                        }

                    vehicleQueries.removeAll()
                    (pending + merged).forEach { insert(it) }
                }
            }
            withContext(Dispatchers.Default) { photosToDelete.forEach { deletePhotoFile(it) } }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun savePhoto(localId: String, bytes: ByteArray, extension: String): String =
        withContext(Dispatchers.Default) {
            fileSystem.createDirectories(photosDir)
            val path = photosDir / "$localId-${Uuid.random()}.$extension"
            fileSystem.write(path) { write(fileCipher.encrypt(bytes)) }
            path.toString()
        }

    override suspend fun readPhoto(path: String): ByteArray? = withContext(Dispatchers.Default) {
        runCatching { fileCipher.decrypt(fileSystem.read(path.toPath()) { readByteArray() }) }.getOrNull()
    }

    override suspend fun deletePhoto(path: String) = withContext(Dispatchers.Default) {
        deletePhotoFile(path)
    }

    override suspend fun clearAll() {
        mutex.withLock {
            database.use { vehicleQueries.removeAll() }
            withContext(Dispatchers.Default) { runCatching { fileSystem.deleteRecursively(baseDir) } }
            legacyChecked = true
        }
    }

    /** Fase 6F: cifra las fotos que la versión anterior dejó sin cifrar (mismo nombre de archivo). */
    suspend fun encryptLegacyPhotos() = withContext(Dispatchers.Default) {
        if (!fileSystem.exists(photosDir)) return@withContext
        fileSystem.list(photosDir)
            .filterNot { it.name.endsWith(".tmp") }
            .forEach { encryptInPlace(fileSystem, fileCipher, it) }
    }

    // ---------------- Internos ----------------

    private fun observe(query: AppDatabase.() -> app.cash.sqldelight.Query<String>): Flow<List<String>> = flow {
        val db = ready()
        emitAll(db.query().asFlow().mapToList(Dispatchers.Default))
    }

    /** Abre la base y, la primera vez, trae los vehículos del archivo de la versión anterior. */
    private suspend fun ready(): AppDatabase {
        val db = database.get()
        if (!legacyChecked) mutex.withLock { if (!legacyChecked) importLegacyFile().also { legacyChecked = true } }
        return db
    }

    /**
     * Primero se guardan en la tabla y después se borra el archivo. Solo se insertan si la tabla está
     * vacía: si la app se cerró entre las dos cosas, la siguiente vez no se duplican ni se pisan cambios.
     */
    private suspend fun importLegacyFile() {
        val legacy = withContext(Dispatchers.Default) {
            if (!fileSystem.exists(legacyFile)) return@withContext null
            runCatching {
                json.decodeFromString(ListSerializer(VehicleEntity.serializer()), fileSystem.read(legacyFile) { readUtf8() })
            }.getOrElse {
                AppLog.w(LOG_TAG, "vehicles.json de la versión anterior estaba dañado; se descarta", it)
                emptyList()
            }
        } ?: return
        database.use {
            vehicleQueries.transaction {
                if (vehicleQueries.countAll().executeAsOne() == 0L) legacy.forEach { insert(it) }
            }
        }
        withContext(Dispatchers.Default) {
            runCatching { fileSystem.delete(legacyFile) }
            runCatching { fileSystem.delete(legacyTemp) }
        }
    }

    private fun AppDatabase.insert(entity: VehicleEntity) {
        vehicleQueries.upsert(
            local_id = entity.localId,
            server_id = entity.serverId,
            sync_state = entity.syncState.name,
            json = json.encodeToString(VehicleEntity.serializer(), entity),
        )
    }

    private fun decode(text: String): VehicleEntity? =
        runCatching { json.decodeFromString(VehicleEntity.serializer(), text) }.getOrNull()

    private fun deletePhotoFile(path: String) {
        runCatching { fileSystem.delete(path.toPath()) }
    }

    private companion object {
        const val LOG_TAG = "BaseLocal"
    }
}
