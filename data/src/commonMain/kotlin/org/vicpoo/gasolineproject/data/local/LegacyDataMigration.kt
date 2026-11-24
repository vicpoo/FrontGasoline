package org.vicpoo.gasolineproject.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.vicpoo.gasolineproject.data.database.LocalDatabase
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.data.vehicles.local.SqlVehicleLocalDataSource
import org.vicpoo.gasolineproject.domain.common.AppLogger
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.common.e
import org.vicpoo.gasolineproject.domain.common.i
import org.vicpoo.gasolineproject.domain.files.AppDirectories

/**
 * Fase 6F: al abrir la primera versión con base local, pasa TODO lo que la versión anterior dejó en
 * archivos sin cifrar a la base cifrada, sin esperar a que el usuario abra cada pantalla:
 * - cachés JSON (viajes en curso y sin enviar, cola de solicitudes, reservas, historial, reportes...),
 * - puntos GPS de cada viaje (.jsonl),
 * - vehículos (vehicles.json),
 * - y cifra en su lugar las fotos pendientes de subir (recibos, odómetro, vehículos).
 *
 * Cada pieza también se migra sola la primera vez que se usa (si el chofer abre la app y va directo al
 * viaje en curso, el viaje ya sale de la base): esto solo adelanta el trabajo. Es idempotente: si la
 * app se cierra a la mitad, la siguiente vez continúa sin duplicar nada. Al terminar se anota en
 * AppMetaEntity para no volver a revisar los archivos en cada arranque.
 *
 * Además borra las copias viejas de listas que ahora van una fila por elemento (historial de viajes y
 * solicitudes de la empresa, ver [OBSOLETE_DOCUMENTS]): son copia de la API y se vuelven a descargar.
 */
class LegacyDataMigration(
    private val database: LocalDatabase,
    private val caches: List<LocalDocumentCache<*>>,
    private val tripStore: TripRecordingLocalStore,
    private val vehicles: SqlVehicleLocalDataSource,
    private val appScope: AppScope,
    private val logger: AppLogger,
    private val fileSystem: FileSystem,
    directories: AppDirectories,
) {
    private val legacyCacheDir = directories.filesDir.toPath() / "cache"

    fun start() {
        appScope.scope.launch { run() }
    }

    suspend fun run() {
        removeObsoleteDocuments()
        if (isDone()) return
        runCatching {
            caches.forEach { it.current() }
            tripStore.importAllLegacyPoints()
            tripStore.encryptLegacyReceipts()
            vehicles.getAll()
            vehicles.encryptLegacyPhotos()
            database.use { appMetaQueries.put(DONE_KEY, DONE_VALUE) }
            logger.i(LOG_TAG, "Datos de la versión anterior pasados a la base local cifrada")
        }.onFailure {
            // No se marca como hecha: se reintenta en el siguiente arranque. Lo ya pasado no se repite.
            logger.e(LOG_TAG, "No se pudieron pasar todos los datos de la versión anterior", it)
        }
    }

    private suspend fun removeObsoleteDocuments() {
        runCatching {
            database.use { OBSOLETE_DOCUMENTS.forEach { cacheDocumentQueries.remove(it) } }
            withContext(Dispatchers.Default) {
                OBSOLETE_DOCUMENTS.forEach { name ->
                    fileSystem.delete(legacyCacheDir / "$name.json")
                    fileSystem.delete(legacyCacheDir / "$name.json.tmp")
                }
            }
        }.onFailure { logger.e(LOG_TAG, "No se pudieron borrar copias viejas de listas", it) }
    }

    private suspend fun isDone(): Boolean =
        database.use { appMetaQueries.valueOf(DONE_KEY).executeAsOneOrNull() } == DONE_VALUE

    companion object {
        const val DONE_KEY = "legacy_files_migrated"
        const val DONE_VALUE = "1"

        /** Listas que antes eran un documento y ahora van una fila por elemento (ListItemEntity). */
        val OBSOLETE_DOCUMENTS = listOf("trips", "company_reservations")
        private const val LOG_TAG = "BaseLocal"
    }
}
