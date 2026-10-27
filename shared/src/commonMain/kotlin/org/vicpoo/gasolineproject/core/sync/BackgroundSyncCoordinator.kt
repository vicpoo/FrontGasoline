package org.vicpoo.gasolineproject.core.sync

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.core.session.SessionReader

/**
 * Enciende la sincronización en segundo plano mientras haya sesión y la apaga al cerrarla
 * (o si el token venció). Se crea al iniciar la app (createdAtStart en Koin).
 */
class BackgroundSyncCoordinator(
    private val sessionReader: SessionReader,
    private val scheduler: BackgroundSyncScheduler,
    appScope: AppScope,
) {
    init {
        appScope.scope.launch {
            sessionReader.session
                .map { it != null }
                .distinctUntilChanged()
                .collect { loggedIn ->
                    if (loggedIn) {
                        scheduler.schedulePeriodic()
                        scheduler.requestWhenOnline() // lo que haya quedado de la vez anterior
                    } else {
                        scheduler.cancel()
                    }
                }
        }
    }
}
