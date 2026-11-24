package org.vicpoo.gasolineproject.domain.sync.usecase

import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask

/**
 * Antes de "Salir": si hay registros que solo existen en el teléfono, intenta mandarlos una vez y regresa
 * cuántos se BORRARÍAN al salir (0 = se puede cerrar sesión sin perder nada).
 * Los viajes se intentan mandar también (así la API recibe la última ruta), pero no cuentan:
 * se quedan guardados en el teléfono para esa cuenta (ver [PendingSyncTask.lostOnLogoutCount]).
 */
class PendingBeforeLogoutUseCase(private val tasks: List<PendingSyncTask>) {
    suspend operator fun invoke(): Int {
        if (tasks.sumOf { it.pendingCount() } == 0) return 0
        SyncAllPendingUseCase(tasks)()
        return tasks.sumOf { it.lostOnLogoutCount() }
    }
}
