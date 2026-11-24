package org.vicpoo.gasolineproject.data.triprecording

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync

/** Lo que ve la pantalla del GPS en cada latido. */
internal data class GpsStatus(
    val gpsActive: Boolean,
    val hasLocationPermission: Boolean,
    val hasPreciseLocation: Boolean,
)

/**
 * Latido del viaje en curso (sale de GpsTripTracker, punto 3.2.3): mientras el GPS está encendido,
 * cada [HEARTBEAT_MS]
 * - revisa si siguen llegando lecturas (más de [GPS_TIMEOUT_MS] sin ninguna = sin señal) y lo avisa en
 *   la notificación del servicio, quitando el aviso cuando vuelve;
 * - vuelve a leer los permisos de ubicación (el usuario pudo cambiarlos en Ajustes);
 * - y cada minuto pide mandar lo pendiente a la API.
 */
internal class GpsSignalWatchdog(
    private val clock: AppClock,
    private val locationProvider: LocationProvider,
    private val trackingService: TrackingServiceController,
    private val sync: TripRecordingSync,
    private val appScope: AppScope,
) {
    private var job: Job? = null
    private var lastFixMillis: Long = 0L

    /** Llegó una lectura del GPS. */
    fun fixReceived() {
        lastFixMillis = clock.nowMillis()
    }

    fun start(onStatus: (GpsStatus) -> Unit) {
        job?.cancel()
        job = appScope.scope.launch {
            var ticks = 0
            var signalLost = false
            while (isActive) {
                delay(HEARTBEAT_MS)
                val active = clock.nowMillis() - lastFixMillis < GPS_TIMEOUT_MS
                onStatus(
                    GpsStatus(
                        gpsActive = active,
                        hasLocationPermission = locationProvider.hasPermission(),
                        hasPreciseLocation = locationProvider.hasPreciseLocation(),
                    ),
                )
                if (signalLost == active) {
                    signalLost = !active
                    trackingService.setGpsSignalLost(signalLost)
                }
                ticks++
                if (ticks % SYNC_EVERY_TICKS == 0) sync.requestSync()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val HEARTBEAT_MS = 5_000L
        const val GPS_TIMEOUT_MS = 30_000L
        const val SYNC_EVERY_TICKS = 12 // 12 x 5 s = cada minuto
    }
}
