package org.vicpoo.gasolineproject.data.triprecording.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.data.database.LocalDatabase
import org.vicpoo.gasolineproject.data.local.LocalDocumentCache
import org.vicpoo.gasolineproject.data.local.encryptInPlace
import org.vicpoo.gasolineproject.database.Track_point
import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.domain.files.AppDirectories
import org.vicpoo.gasolineproject.domain.files.FileCipher
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Almacenamiento offline del registro de viajes, todo dentro de la base local cifrada salvo las fotos:
 * - documento "trip_recording": viajes y recargas pendientes ([LocalDocumentCache]).
 * - tabla track_point: puntos GPS, uno por fila (agregar es barato aunque haya miles).
 * - receipts/: fotos de recibos y odómetro pendientes de subir, cifradas con [FileCipher].
 *
 * Fase 6F: los puntos de la versión anterior (points/<viaje>.jsonl) se pasan a la base la primera vez
 * que se usa ese viaje ([importLegacyPoints]); las fotos viejas sin cifrar se siguen leyendo y
 * [encryptLegacyReceipts] las cifra.
 */
class TripRecordingLocalStore(
    private val fileSystem: FileSystem,
    directories: AppDirectories,
    private val json: Json,
    private val snapshotCache: LocalDocumentCache<RecordingSnapshot>,
    private val database: LocalDatabase,
    private val fileCipher: FileCipher,
) {
    private val baseDir: Path = directories.filesDir.toPath() / "trip_recording"
    private val legacyPointsDir: Path = baseDir / "points"
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
        importLegacyPoints(tripLocalId)
        database.use {
            trackPointQueries.transaction {
                val seq = trackPointQueries.nextSeq(tripLocalId).executeAsOneOrNull() ?: 0L
                trackPointQueries.insert(point.toRow(tripLocalId, seq))
            }
        }
    }

    /** Puntos a partir del número [from] (para mandarlos en lotes): consulta por llave, sin OFFSET. */
    suspend fun readPoints(tripLocalId: String, from: Int = 0, limit: Int = Int.MAX_VALUE): List<TrackPointEntity> =
        pointsMutex.withLock {
            importLegacyPoints(tripLocalId)
            database.use {
                trackPointQueries.pageForTrip(tripLocalId, from.toLong(), limit.toLong())
                    .executeAsList()
                    .map { it.toEntity() }
            }
        }

    suspend fun countPoints(tripLocalId: String): Int = pointsMutex.withLock {
        importLegacyPoints(tripLocalId)
        database.use { trackPointQueries.countForTrip(tripLocalId).executeAsOne().toInt() }
    }

    suspend fun readRoute(tripLocalId: String): List<GeoPoint> =
        readPoints(tripLocalId).map { GeoPoint(it.lat, it.lng) }

    /**
     * Pasa a la base los puntos que la versión anterior guardó en points/<viaje>.jsonl y borra el archivo.
     * Solo inserta si la base todavía no tiene puntos de ese viaje: si la app se cerró entre insertar y
     * borrar, la siguiente vez no se duplican. Debe llamarse con [pointsMutex] tomado.
     */
    private suspend fun importLegacyPoints(tripLocalId: String) {
        val file = legacyPointsFile(tripLocalId)
        val points = withContext(Dispatchers.Default) {
            if (!fileSystem.exists(file)) return@withContext null
            runCatching { fileSystem.read(file) { readUtf8() } }.getOrDefault("")
                .lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { runCatching { json.decodeFromString(TrackPointEntity.serializer(), it) }.getOrNull() }
                .toList()
        } ?: return
        database.use {
            trackPointQueries.transaction {
                if (trackPointQueries.countForTrip(tripLocalId).executeAsOne() == 0L) {
                    points.forEachIndexed { index, point -> trackPointQueries.insert(point.toRow(tripLocalId, index.toLong())) }
                }
            }
        }
        withContext(Dispatchers.Default) { runCatching { fileSystem.delete(file) } }
    }

    /** Fase 6F: pasa a la base los puntos de todos los viajes de la versión anterior (al abrir la app). */
    suspend fun importAllLegacyPoints() {
        val tripIds = withContext(Dispatchers.Default) {
            if (!fileSystem.exists(legacyPointsDir)) return@withContext emptyList()
            fileSystem.list(legacyPointsDir).filter { it.name.endsWith(".jsonl") }.map { it.name.removeSuffix(".jsonl") }
        }
        tripIds.forEach { tripLocalId -> pointsMutex.withLock { importLegacyPoints(tripLocalId) } }
    }

    // ---------------- Recibos y fotos del odómetro (cifrados) ----------------

    @OptIn(ExperimentalUuidApi::class)
    suspend fun saveReceipt(bytes: ByteArray, extension: String): String = withContext(Dispatchers.Default) {
        fileSystem.createDirectories(receiptsDir)
        val path = receiptsDir / "recibo-${Uuid.random()}.$extension"
        fileSystem.write(path) { write(fileCipher.encrypt(bytes)) }
        path.toString()
    }

    /** La foto ya descifrada; null si no existe o no se puede descifrar (se perdió la llave). */
    suspend fun readReceipt(path: String): ByteArray? = withContext(Dispatchers.Default) {
        runCatching { fileCipher.decrypt(fileSystem.read(path.toPath()) { readByteArray() }) }.getOrNull()
    }

    /** Fase 6F: cifra las fotos que la versión anterior dejó sin cifrar (mismo nombre: los viajes las siguen encontrando). */
    suspend fun encryptLegacyReceipts() = withContext(Dispatchers.Default) {
        if (!fileSystem.exists(receiptsDir)) return@withContext
        fileSystem.list(receiptsDir)
            .filterNot { it.name.endsWith(".tmp") }
            .forEach { path -> encryptInPlace(fileSystem, fileCipher, path) }
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
            database.use { trackPointQueries.deleteForTrip(tripLocalId) }
            withContext(Dispatchers.Default) { runCatching { fileSystem.delete(legacyPointsFile(tripLocalId)) } }
        }
    }

    /** Al cerrar sesión. */
    suspend fun clearAll() {
        mutex.withLock { snapshotCache.clear() }
        pointsMutex.withLock { database.use { trackPointQueries.deleteAll() } }
        withContext(Dispatchers.Default) { runCatching { fileSystem.deleteRecursively(baseDir) } }
    }

    private fun legacyPointsFile(tripLocalId: String): Path = legacyPointsDir / "$tripLocalId.jsonl"
}

private fun TrackPointEntity.toRow(tripLocalId: String, seq: Long) = Track_point(
    trip_local_id = tripLocalId,
    seq = seq,
    lat = lat,
    lng = lng,
    t = t,
    acc = acc,
    alt = alt,
    spd = spd,
)

private fun Track_point.toEntity() = TrackPointEntity(lat = lat, lng = lng, t = t, acc = acc, alt = alt, spd = spd)
