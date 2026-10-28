package org.vicpoo.gasolineproject.core.sync

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.core.network.ConnectivityMonitor
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationWillEnterForegroundNotification

/**
 * iOS no garantiza ejecutar trabajo periódico en segundo plano (BGTaskScheduler decide cuándo).
 * Por eso aquí la sincronización segura es mientras la app está abierta:
 * - al iniciar sesión / abrir la app (BackgroundSyncCoordinator llama schedulePeriodic + requestWhenOnline),
 * - cada vez que la app vuelve a primer plano,
 * - cuando vuelve la red (BackgroundSyncCoordinator) y cuando una feature guarda algo sin red.
 *
 * [syncAll] se pide a Koin hasta que se usa: así no hay ciclo (las tareas de sincronización
 * necesitan este planificador para crearse).
 */
class IosSyncScheduler(
    private val appScope: AppScope,
    private val connectivity: ConnectivityMonitor,
    private val syncAll: () -> SyncAllPendingUseCase,
) : BackgroundSyncScheduler {

    private val running = Mutex()
    private var loggedIn = false
    private var foregroundObserver: Any? = null

    override fun schedulePeriodic() {
        loggedIn = true
        if (foregroundObserver != null) return
        foregroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationWillEnterForegroundNotification,
            null,
            NSOperationQueue.mainQueue,
        ) { _ -> requestWhenOnline() }
    }

    override fun requestWhenOnline() {
        // Sin red no se intenta: BackgroundSyncCoordinator lo vuelve a pedir al reconectar
        if (!loggedIn || !connectivity.isOnline.value) return
        appScope.scope.launch {
            // Si ya hay una sincronización en curso, esa se encarga (evita vueltas repetidas)
            if (!running.tryLock()) return@launch
            try {
                syncAll()()
            } finally {
                running.unlock()
            }
        }
    }

    override fun cancel() {
        loggedIn = false
        foregroundObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        foregroundObserver = null
    }
}
