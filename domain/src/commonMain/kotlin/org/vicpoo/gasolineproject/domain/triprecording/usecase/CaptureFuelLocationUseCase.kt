package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

/**
 * Punto donde se está haciendo la carga de gasolina. Funciona sin internet (el GPS no usa red).
 * Orden de preferencia:
 * 1. La lectura actual del GPS del viaje, si es reciente (aunque el filtro de ruta la haya
 *    descartado por estar detenido: justo es el caso de la gasolinera).
 * 2. La última ubicación del teléfono, si tiene menos de [MAX_LAST_KNOWN_AGE_MS] y buena precisión.
 * 3. La última lectura del GPS del viaje (aunque no sea reciente).
 * 4. El último punto aceptado de la ruta.
 * null = no hay nada confiable; la API la completa con el punto de la ruta más cercano a la hora.
 */
class CaptureFuelLocationUseCase(
    private val tracker: TripTracker,
    private val locationProvider: LocationProvider,
    private val clock: AppClock,
) {
    suspend operator fun invoke(): GeoPoint? {
        val session = tracker.session.value
        if (session.gpsActive && session.current != null) return session.current

        val lastKnown = runCatching { locationProvider.lastKnown() }.getOrNull()
        if (lastKnown != null &&
            clock.nowMillis() - lastKnown.timeMillis <= MAX_LAST_KNOWN_AGE_MS &&
            (lastKnown.accuracyMeters ?: 0.0) <= MAX_ACCURACY_M
        ) {
            return lastKnown.point
        }
        return session.current ?: session.trip?.lastLocation
    }

    private companion object {
        const val MAX_LAST_KNOWN_AGE_MS = 2 * 60_000L
        const val MAX_ACCURACY_M = 50.0
    }
}
