package org.vicpoo.gasolineproject.domain.sync.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import org.vicpoo.gasolineproject.domain.sync.PendingSummaryItem
import org.vicpoo.gasolineproject.domain.sync.PendingSyncKind
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask

/**
 * Pendientes de toda la app agrupados por [PendingSyncKind] (solo los grupos con algo).
 * Cada feature guarda en su propio archivo, así que se vuelve a contar cada [intervalMillis]
 * mientras alguien escucha (contar es leer la copia en memoria: es barato).
 */
class ObservePendingSummaryUseCase(private val tasks: List<PendingSyncTask>) {
    operator fun invoke(intervalMillis: Long = DEFAULT_INTERVAL_MS): Flow<List<PendingSummaryItem>> = flow {
        while (true) {
            emit(summary())
            delay(intervalMillis)
        }
    }.distinctUntilChanged()

    suspend fun summary(): List<PendingSummaryItem> =
        tasks.groupBy { it.kind }
            .map { (kind, group) -> PendingSummaryItem(kind, group.sumOf { it.pendingCount() }) }
            .filter { it.count > 0 }
            .sortedBy { it.kind.ordinal }

    private companion object {
        const val DEFAULT_INTERVAL_MS = 5_000L
    }
}
