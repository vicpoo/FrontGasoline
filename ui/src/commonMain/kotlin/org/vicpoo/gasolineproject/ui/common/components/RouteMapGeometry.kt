package org.vicpoo.gasolineproject.ui.common.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tan
import org.vicpoo.gasolineproject.domain.common.GeoPoint

/* Cuentas del mapa de ruta: proyección y zoom, área visible y simplificación de la línea (punto 3.2.3). */

/** Puntos que estén a menos de esto (px) de la línea se quitan: a simple vista la ruta es igual. */
internal const val SIMPLIFY_TOLERANCE_PX = 1.5f

/**
 * Acercamiento en curso: un punto p (en pixeles de la vista completa) se dibuja en
 * (p - focus) * scale + center. [translation] es lo mismo expresado como p * scale + translation.
 */
internal class ZoomTransform(val scale: Float, val focus: Offset, val center: Offset) {
    val translation: Offset get() = center - focus * scale

    fun apply(p: Offset): Offset = (p - focus) * scale + center
}

/** Proyección Web Mercator (la de OpenStreetMap/Google). */
internal class Viewport(val zoom: Int, val left: Double, val top: Double) {

    fun toScreen(p: GeoPoint): Offset {
        val (x, y) = project(p, zoom)
        return Offset((x - left).toFloat(), (y - top).toFloat())
    }

    fun visibleTiles(width: Double, height: Double, extra: Double = 0.0): List<Tile> {
        val tileCount = 1 shl zoom
        val firstX = floor(left / TILE_SIZE).toInt()
        val lastX = floor((left + width) / TILE_SIZE).toInt()
        // Con [extra] > 0 se cargan teselas de más arriba y abajo: si el mapa se desplaza
        // (hoja de detalles) no queda hueco gris
        val firstY = floor((top - extra) / TILE_SIZE).toInt()
        val lastY = floor((top + height + extra) / TILE_SIZE).toInt()
        val tiles = ArrayList<Tile>()
        for (tx in firstX..lastX) {
            for (ty in firstY..lastY) {
                if (ty < 0 || ty >= tileCount) continue
                val wrappedX = ((tx % tileCount) + tileCount) % tileCount
                tiles += Tile(
                    url = "https://tile.openstreetmap.org/$zoom/$wrappedX/$ty.png",
                    offsetX = (tx * TILE_SIZE - left).roundToInt(),
                    offsetY = (ty * TILE_SIZE - top).roundToInt(),
                )
            }
        }
        return tiles
    }

    companion object {
        /**
         * Zoom más grande que deja ver toda la ruta con margen dentro de [width] x [fitHeight];
         * la ruta queda centrada en ese alto (la parte de arriba del mapa).
         */
        fun fit(points: List<GeoPoint>, width: Double, height: Double, fitHeight: Double = height): Viewport {
            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLng = points.minOf { it.lng }
            val maxLng = points.maxOf { it.lng }

            val padding = 0.75
            var zoom = MAX_ZOOM
            while (zoom > 2) {
                val (x1, y1) = project(GeoPoint(maxLat, minLng), zoom)
                val (x2, y2) = project(GeoPoint(minLat, maxLng), zoom)
                if ((x2 - x1) <= width * padding && (y2 - y1) <= fitHeight * padding) break
                zoom--
            }
            val (cx, cy) = project(GeoPoint((minLat + maxLat) / 2, (minLng + maxLng) / 2), zoom)
            return Viewport(zoom, cx - width / 2, cy - fitHeight / 2)
        }

        /** [point] en el centro del área de [width] x [fitHeight] (la parte de arriba del mapa). */
        fun centeredOn(point: GeoPoint, zoom: Int, width: Double, fitHeight: Double): Viewport {
            val (cx, cy) = project(point, zoom)
            return Viewport(zoom, cx - width / 2, cy - fitHeight / 2)
        }

        fun project(p: GeoPoint, zoom: Int): Pair<Double, Double> {
            val scale = TILE_SIZE * (1 shl zoom)
            val x = (p.lng + 180.0) / 360.0 * scale
            val latRad = p.lat * PI / 180.0
            val y = (1.0 - ln(tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / PI) / 2.0 * scale
            return x to y
        }
    }
}

/**
 * Douglas-Peucker (iterativo, sin recursión para rutas largas): deja solo los puntos que cambian
 * la forma de la línea en más de [tolerance] px. Una ruta de miles de puntos queda en decenas.
 */
internal fun simplify(points: List<Offset>, tolerance: Float): List<Offset> {
    if (points.size < 3) return points
    val keep = BooleanArray(points.size)
    keep[0] = true
    keep[points.lastIndex] = true
    val stack = ArrayDeque<Pair<Int, Int>>()
    stack.addLast(0 to points.lastIndex)
    while (stack.isNotEmpty()) {
        val (start, end) = stack.removeLast()
        var maxDistance = 0f
        var index = -1
        for (i in start + 1 until end) {
            val d = distanceToSegment(points[i], points[start], points[end])
            if (d > maxDistance) {
                maxDistance = d
                index = i
            }
        }
        if (index != -1 && maxDistance > tolerance) {
            keep[index] = true
            stack.addLast(start to index)
            stack.addLast(index to end)
        }
    }
    return points.filterIndexed { i, _ -> keep[i] }
}

/** Largo acumulado de la línea hasta cada punto (px). */
internal fun cumulativeLengths(points: List<Offset>): FloatArray {
    val result = FloatArray(points.size)
    for (i in 1 until points.size) result[i] = result[i - 1] + (points[i] - points[i - 1]).getDistance()
    return result
}

internal fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val lengthSq = dx * dx + dy * dy
    if (lengthSq == 0f) return sqrt((p.x - a.x) * (p.x - a.x) + (p.y - a.y) * (p.y - a.y))
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSq).coerceIn(0f, 1f)
    val projX = a.x + t * dx
    val projY = a.y + t * dy
    return abs(sqrt((p.x - projX) * (p.x - projX) + (p.y - projY) * (p.y - projY)))
}
