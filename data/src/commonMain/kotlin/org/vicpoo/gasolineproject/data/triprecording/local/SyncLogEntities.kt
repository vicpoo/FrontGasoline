package org.vicpoo.gasolineproject.data.triprecording.local

import kotlinx.serialization.Serializable

/** Un paso de la bitácora de sincronización (archivo sync_log.json). */
@Serializable
data class SyncLogEntryEntity(
    val at: String,
    val atMillis: Long,
    val step: String,
    /** Nombre de SyncLogResult: OK | OFFLINE | REJECTED | ERROR. */
    val result: String,
    val message: String? = null,
    val code: Int? = null,
    val durationMs: Long = 0,
)

/** Últimos pasos de sincronización, el más reciente primero. */
@Serializable
data class SyncLogSnapshot(
    val entries: List<SyncLogEntryEntity> = emptyList(),
)
