package org.vicpoo.gasolineproject.core.location

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.core.common.GeoPoint

/** Una lectura del GPS del teléfono. */
data class LocationFix(
    val lat: Double,
    val lng: Double,
    /** Radio de error en metros (null si el GPS no lo da). */
    val accuracyMeters: Double? = null,
    val altitudeMeters: Double? = null,
    val speedMetersPerSecond: Double? = null,
    val timeMillis: Long,
) {
    val point: GeoPoint get() = GeoPoint(lat, lng)
}

/**
 * Fuente de ubicación de cada plataforma (Android: LocationManager; iOS: pendiente).
 * La lógica del viaje solo conoce esta interfaz (Dependency Inversion).
 */
interface LocationProvider {
    /** true si el usuario ya dio permiso de ubicación. */
    fun hasPermission(): Boolean

    /** true si el GPS / ubicación del teléfono está encendido. */
    fun isLocationEnabled(): Boolean

    /**
     * Lecturas continuas. Se detienen al cancelar el Flow.
     * Si no hay permiso, el Flow termina sin emitir.
     */
    fun updates(intervalMillis: Long, minDistanceMeters: Float): Flow<LocationFix>

    /** Última ubicación conocida (rápida, puede ser vieja o null). */
    suspend fun lastKnown(): LocationFix?
}

/**
 * Servicio en primer plano (Android) que mantiene vivo el GPS con la pantalla apagada.
 * Solo muestra la notificación "Viaje en curso"; el registro lo hace el TripTracker.
 */
interface TrackingServiceController {
    fun start()
    fun stop()
}
