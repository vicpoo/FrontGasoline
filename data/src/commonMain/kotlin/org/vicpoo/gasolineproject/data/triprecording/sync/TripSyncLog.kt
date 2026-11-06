package org.vicpoo.gasolineproject.data.triprecording.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.data.local.JsonFileCache
import org.vicpoo.gasolineproject.data.triprecording.local.SyncLogEntryEntity
import org.vicpoo.gasolineproject.data.triprecording.local.SyncLogSnapshot
import org.vicpoo.gasolineproject.domain.triprecording.SyncLogEntry
import org.vicpoo.gasolineproject.domain.triprecording.SyncLogResult

/**
 * Bitácora de los últimos [MAX_ENTRIES] pasos de sincronización, guardada en el teléfono.
 * Permite ver después qué falló, cuándo y cuánto tardó (por ejemplo, "la foto tardó 30 s y se cortó").
 */
class TripSyncLog(
    private val cache: JsonFileCache<SyncLogSnapshot>,
    private val clock: AppClock,
) {
    private val mutex = Mutex()

    val entries: Flow<List<SyncLogEntry>> = cache.data.map { snapshot -> snapshot.entries.map { it.toDomain() } }

    fun nowMillis(): Long = clock.nowMillis()

    suspend fun record(
        step: String,
        result: SyncLogResult,
        message: String? = null,
        code: Int? = null,
        startedAtMillis: Long? = null,
    ) {
        val now = clock.nowMillis()
        val entry = SyncLogEntryEntity(
            at = clock.toLocalIso(now),
            atMillis = now,
            step = step,
            result = result.name,
            message = message,
            code = code,
            durationMs = startedAtMillis?.let { (now - it).coerceAtLeast(0) } ?: 0,
        )
        mutex.withLock {
            val current = cache.current()
            cache.save(SyncLogSnapshot(entries = (listOf(entry) + current.entries).take(MAX_ENTRIES)))
        }
    }

    suspend fun clear() = mutex.withLock { cache.clear() }

    private fun SyncLogEntryEntity.toDomain() = SyncLogEntry(
        at = at,
        atMillis = atMillis,
        step = step,
        result = SyncLogResult.entries.firstOrNull { it.name == result } ?: SyncLogResult.ERROR,
        message = message,
        code = code,
        durationMs = durationMs,
    )

    private companion object {
        const val MAX_ENTRIES = 150
    }
}
