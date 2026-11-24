package org.vicpoo.gasolineproject.domain.sync.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.sync.PendingSyncKind
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.domain.sync.stopsSync

/**
 * Ejecuta todas las [PendingSyncTask] registradas, en el orden de [PendingSyncKind]
 * (viajes y recargas primero). Se detiene en el primer "sin red / sesión vencida".
 */
class SyncAllPendingUseCase(private val tasks: List<PendingSyncTask>) {
    suspend operator fun invoke(): AppResult<Unit> {
        for (task in tasks.sortedBy { it.kind.ordinal }) {
            val result = task.syncPending()
            if (result is AppResult.Failure && result.error.stopsSync()) return result
        }
        return AppResult.Success(Unit)
    }
}
