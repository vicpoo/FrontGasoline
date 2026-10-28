package org.vicpoo.gasolineproject.feature.triprecording.data

import org.vicpoo.gasolineproject.core.common.GeoMath
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.location.LocationFix

/**
 * Filtro de la ruta del viaje: decide qué lecturas del GPS cuentan y cuánta distancia suman.
 * Es lógica pura (sin GPS, archivos ni corrutinas), así que se prueba sola (RouteFilterTest).
 *
 * Una lectura NO cuenta si:
 * - tiene mucho error (más de [MAX_ACCURACY_M], igual que el filtro de la API),
 * - está a menos de [MIN_STEP_M] del punto anterior (ruido estando detenido),
 * - implica una velocidad imposible (más de [MAX_SPEED_MPS], un salto del GPS).
 */
internal class RouteFilter {

    private var lastPoint: GeoPoint? = null
    private var lastMillis: Long = 0L

    /**
     * Empieza de nuevo (al iniciar, reanudar o deshacer el alto): lo que se movió mientras tanto
     * no se suma. [from] = último punto conocido (al restaurar un viaje después de cerrar la app).
     */
    fun reset(from: GeoPoint? = null) {
        lastPoint = from
        lastMillis = 0L
    }

    /** null si la lectura no cuenta; si cuenta, los metros que suma a la distancia (0 en la primera). */
    fun accept(fix: LocationFix): Double? {
        if ((fix.accuracyMeters ?: 0.0) > MAX_ACCURACY_M) return null

        val previous = lastPoint
        var added = 0.0
        if (previous != null) {
            val meters = GeoMath.distanceMeters(previous, fix.point)
            if (meters < MIN_STEP_M) return null
            val seconds = ((fix.timeMillis - lastMillis).coerceAtLeast(1L)) / 1000.0
            if (meters / seconds > MAX_SPEED_MPS) return null
            added = meters
        }

        lastPoint = fix.point
        lastMillis = fix.timeMillis
        return added
    }

    companion object {
        const val MAX_ACCURACY_M = 50.0
        const val MIN_STEP_M = 3.0
        const val MAX_SPEED_MPS = 70.0 // ~250 km/h
    }
}
