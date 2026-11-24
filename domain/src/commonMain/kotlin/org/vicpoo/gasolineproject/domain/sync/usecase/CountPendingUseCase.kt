package org.vicpoo.gasolineproject.domain.sync.usecase

import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask

/** Cuántos registros de TODA la app siguen solo en el teléfono. */
class CountPendingUseCase(private val tasks: List<PendingSyncTask>) {
    suspend operator fun invoke(): Int = tasks.sumOf { it.pendingCount() }
}
