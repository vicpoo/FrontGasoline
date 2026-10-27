package org.vicpoo.gasolineproject.feature.triprecording.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.platform.AppDirectories
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Almacenamiento offline del registro de viajes:
 * - recording.json: viajes y recargas pendientes (con escritura atómica de JsonFileCache).
 * - points/<viaje>.jsonl: puntos GPS, una línea por punto (agregar es barato aunque haya miles).
 * - receipts/: fotos de los recibos pendientes de subir.
 */
class TripRecordingLocalStore(
    private val fileSystem: FileSystem,
    directories: AppDirectories,
    private val json: Json,
    private val snapshotCache: JsonFileCache<RecordingSnapshot>,
) {
    private val baseDir: Path = directories.filesDir.toPath() / "trip_recording"
    private val pointsDir: Path = baseDir / "points"
    private val receiptsDir: Path = baseDir / "receipts"

    /** Protege el leer-modificar-guardar del snapshot (GPS, formulario y sincronización a la vez). */
    private val mutex = Mutex()
    private val pointsMutex = Mutex()

    val snapshot: Flow<RecordingSnapshot> = snapshotCache.data

    suspend fun current(): RecordingSnapshot = snapshotCache.current()

    /** Cambia el snapshot de forma atómica y regresa el resultado. */
    suspend fun update(transform: (RecordingSnapshot) -> RecordingSnapshot): RecordingSnapshot =
        mutex.withLock {
            val updated = transform(snapshotCache.current())
            snapshotCache.save(updated)
            updated
        }

    suspend fun updateTrip(localId: String, transform: (TripEntity) -> TripEntity): TripEntity? {
        var result: TripEntity? = null
        update { snapshot ->
            snapshot.copy(
                trips = snapshot.trips.map { trip ->
                    if (trip.localId == localId) transform(trip).also { result = it } else trip
                },
            )
        }
        return result
    }

    suspend fun updateFuel(localId: String, transform: (FuelEntity) -> FuelEntity): FuelEntity? {
        var result: FuelEntity? = null
        update { snapshot ->
            snapshot.copy(
                fuel = snapshot.fuel.map { fuel ->
                    if (fuel.localId == localId) transform(fuel).also { result = it } else fuel
                },
            )
        }
        return result
    }

    // ---------------- Puntos GPS ----------------

    suspend fun appendPoint(tripLocalId: String, point: TrackPointEntity) = pointsMutex.withLock {
        withContext(Dispatchers.Default) {
            fileSystem.createDirectories(pointsDir)
            val line = json.encodeToString(TrackPointEntity.serializer(), point) + "\n"
            fileSystem.appendingSink(pointsFile(tripLocalId)).buffer().use { it.writeUtf8(line) }
        }
    }

    /** Puntos a partir de [from] (para mandarlos en lotes). */
    suspend fun readPoints(tripLocalId: String, from: Int = 0, limit: Int = Int.MAX_VALUE): List<TrackPointEntity> =
        pointsMutex.withLock {
            withContext(Dispatchers.Default) {
                val file = pointsFile(tripLocalId)
                if (!fileSystem.exists(file)) return@withContext emptyList()
                fileSystem.read(file) { readUtf8() }
                    .lineSequence()
                    .filter { it.isNotBlank() }
                    .drop(from)
                    .take(limit)
                    .mapNotNull { runCatching { json.decodeFromString(TrackPointEntity.serializer(), it) }.getOrNull() }
                    .toList()
            }
        }

    suspend fun countPoints(tripLocalId: String): Int = pointsMutex.withLock {
        withContext(Dispatchers.Default) {
            val file = pointsFile(tripLocalId)
            if (!fileSystem.exists(file)) 0 else fileSystem.read(file) { readUtf8() }.lineSequence().count { it.isNotBlank() }
        }
    }

    suspend fun readRoute(tripLocalId: String): List<GeoPoint> =
        readPoints(tripLocalId).map { GeoPoint(it.lat, it.lng) }

    // ---------------- Recibos ----------------

    @OptIn(ExperimentalUuidApi::class)
    suspend fun saveReceipt(bytes: ByteArray, extension: String): String = withContext(Dispatchers.Default) {
        fileSystem.createDirectories(receiptsDir)
        val path = receiptsDir / "recibo-${Uuid.random()}.$extension"
        fileSystem.write(path) { write(bytes) }
        path.toString()
    }

    suspend fun readReceipt(path: String): ByteArray? = withContext(Dispatchers.Default) {
        runCatching { fileSystem.read(path.toPath()) { readByteArray() } }.getOrNull()
    }

    suspend fun deleteFile(path: String?) {
        if (path == null) return
        withContext(Dispatchers.Default) { runCatching { fileSystem.delete(path.toPath()) } }
    }

    /** Borra el viaje terminado y enviado: sus recargas, puntos y fotos. */
    suspend fun removeTrip(tripLocalId: String) {
        var receipts: List<String?> = emptyList()
        update { snapshot ->
            receipts = snapshot.fuel.filter { it.tripLocalId == tripLocalId }.map { it.receiptPath }
            snapshot.copy(
                trips = snapshot.trips.filterNot { it.localId == tripLocalId },
                fuel = snapshot.fuel.filterNot { it.tripLocalId == tripLocalId },
            )
        }
        receipts.forEach { deleteFile(it) }
        pointsMutex.withLock {
            withContext(Dispatchers.Default) { runCatching { fileSystem.delete(pointsFile(tripLocalId)) } }
        }
    }

    /** Al cerrar sesión. */
    suspend fun clearAll() {
        mutex.withLock { snapshotCache.clear() }
        withContext(Dispatchers.Default) { runCatching { fileSystem.deleteRecursively(baseDir) } }
    }

    private fun pointsFile(tripLocalId: String): Path = pointsDir / "$tripLocalId.jsonl"
}
