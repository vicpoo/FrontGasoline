package org.vicpoo.gasolineproject.domain.sync.usecase

import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask

/**
 * Antes de "Salir": si hay registros que solo existen en el teléfono y que se BORRAN al salir
 * (solicitudes, vehículos...), intenta mandarlos una vez y regresa cuántos se perderían
 * (0 = se puede cerrar sesión sin perder nada).
 *
 * Los viajes NO se mandan aquí: no se borran al salir (se guardan para esa cuenta y se envían al volver
 * a entrar, ver [PendingSyncTask.lostOnLogoutCount]) y ya se intentan mandar al guardarlos. Mandarlos
 * aquí hacía esperar al usuario todo el envío de puntos GPS y fotos (o sus timeouts) antes de salir.
 */
class PendingBeforeLogoutUseCase(private val tasks: List<PendingSyncTask>) {
    suspend operator fun invoke(): Int {
        val atRisk = tasks.filter { it.lostOnLogoutCount() > 0 }
        if (atRisk.isEmpty()) return 0
        SyncAllPendingUseCase(atRisk)()
        return atRisk.sumOf { it.lostOnLogoutCount() }
    }
}
