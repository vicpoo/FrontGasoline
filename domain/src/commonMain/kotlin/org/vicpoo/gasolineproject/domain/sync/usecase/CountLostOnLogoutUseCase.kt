package org.vicpoo.gasolineproject.domain.sync.usecase

import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask

/** Cuántos registros sin enviar se borrarían si la sesión de esta cuenta se descarta (los viajes no cuentan). */
class CountLostOnLogoutUseCase(private val tasks: List<PendingSyncTask>) {
    suspend operator fun invoke(): Int = tasks.sumOf { it.lostOnLogoutCount() }
}
