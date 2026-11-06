package org.vicpoo.gasolineproject.domain.sync

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.connectivity.ConnectivityMonitor
import org.vicpoo.gasolineproject.domain.connectivity.reconnections
import org.vicpoo.gasolineproject.domain.session.SessionReader

/**
 * Enciende la sincronización en segundo plano mientras haya sesión y la apaga al cerrarla
 * (o si el token venció). Se crea al iniciar la app (createdAtStart en Koin).
 *
 * Además, cada vez que vuelve la red con sesión abierta pide enviar lo pendiente. En Android
 * WorkManager ya lo haría solo (es inofensivo pedirlo otra vez); en iOS es lo que dispara el envío.
 */
class BackgroundSyncCoordinator(
    private val sessionReader: SessionReader,
    private val scheduler: BackgroundSyncScheduler,
    private val connectivity: ConnectivityMonitor,
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
        appScope.scope.launch {
            connectivity.reconnections().collect {
                if (sessionReader.current() != null) scheduler.requestWhenOnline()
            }
        }
    }
}
