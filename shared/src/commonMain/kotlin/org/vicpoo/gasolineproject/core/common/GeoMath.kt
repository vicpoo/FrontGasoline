package org.vicpoo.gasolineproject.core.common

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Cálculos geográficos simples (sin dependencias). */
object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Distancia en metros entre dos puntos (fórmula de Haversine). */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = (b.lat - a.lat).toRadians()
        val dLng = (b.lng - a.lng).toRadians()
        val h = sin(dLat / 2).let { it * it } +
            cos(a.lat.toRadians()) * cos(b.lat.toRadians()) * sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    private fun Double.toRadians() = this * kotlin.math.PI / 180.0
}
