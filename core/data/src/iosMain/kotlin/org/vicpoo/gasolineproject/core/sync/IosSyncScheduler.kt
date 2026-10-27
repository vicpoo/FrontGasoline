package org.vicpoo.gasolineproject.core.sync

/**
 * iOS: por ahora no hay trabajo en segundo plano (BGTaskScheduler requiere configurar el
 * Info.plist del iosApp). Lo pendiente se envía al abrir la app y al guardar cambios.
 */
class IosSyncScheduler : BackgroundSyncScheduler {
    override fun schedulePeriodic() = Unit
    override fun requestWhenOnline() = Unit
    override fun cancel() = Unit
}
