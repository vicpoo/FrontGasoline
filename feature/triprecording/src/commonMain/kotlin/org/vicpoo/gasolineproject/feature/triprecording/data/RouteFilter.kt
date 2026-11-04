package org.vicpoo.gasolineproject.feature.triprecording.data

import org.vicpoo.gasolineproject.core.common.GeoMath
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.location.LocationFix

/**
 * Filtro de la ruta del viaje: decide qué lecturas del GPS cuentan y cuánta distancia suman.
 * Es lógica pura (sin GPS, archivos ni corrutinas), así que se prueba sola (RouteFilterTest).
 *
 * El GPS se pide cada segundo y sin distancia mínima (como Strava): entre más seguido, menos se
 * "cortan" las curvas. Por eso aquí se limpia el ruido. Una lectura NO cuenta si:
 * - tiene mucho error (más de [MAX_ACCURACY_M], igual que el filtro de la API),
 * - el propio GPS dice que el carro está detenido (menos de [STOPPED_SPEED_MPS], igual que la API),
 * - está más cerca del punto anterior que su propio margen de error (ver [minStepMeters]),
 * - implica una velocidad imposible (más de [MAX_SPEED_MPS], un salto del GPS).
 *
 * Una lectura descartada NO mueve el punto anterior: la siguiente buena se mide desde el último punto
 * válido, así que descartar no "pierde" distancia en línea recta.
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
            // Detenido (semáforo, gasolinera): el GPS "tiembla" aunque el carro no se mueva
            val speed = fix.speedMetersPerSecond
            if (speed != null && speed < STOPPED_SPEED_MPS) return null

            val meters = GeoMath.distanceMeters(previous, fix.point)
            if (meters < minStepMeters(fix.accuracyMeters)) return null
            val seconds = ((fix.timeMillis - lastMillis).coerceAtLeast(1L)) / 1000.0
            if (meters / seconds > MAX_SPEED_MPS) return null
            added = meters
        }

        lastPoint = fix.point
        lastMillis = fix.timeMillis
        return added
    }

    companion object {
        const val MAX_ACCURACY_M = 25.0
        const val MIN_STEP_M = 3.0

        /** El paso mínimo crece con el error de la lectura, hasta este tope. */
        const val MAX_ADAPTIVE_STEP_M = 8.0

        /** ~3 km/h: VelocidadMovimientoKmh de la API (ParametrosRuta). */
        const val STOPPED_SPEED_MPS = 0.8

        /** 200 km/h: VelocidadMaximaKmh de la API (ParametrosRuta). */
        const val MAX_SPEED_MPS = 200.0 / 3.6

        /**
         * Metros mínimos desde el punto anterior para que una lectura cuente: la mitad de su margen de
         * error, entre [MIN_STEP_M] y [MAX_ADAPTIVE_STEP_M]. Con buena señal (±5 m) basta 3 m; con señal
         * regular (±16 m o más) se piden 8 m para que el temblor no sume.
         */
        fun minStepMeters(accuracyMeters: Double?): Double =
            ((accuracyMeters ?: 0.0) / 2.0).coerceIn(MIN_STEP_M, MAX_ADAPTIVE_STEP_M)
    }
}
