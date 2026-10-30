package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tan

private const val TILE_SIZE = 256.0
private const val MAX_ZOOM = 17

/** Puntos que estén a menos de esto (px) de la línea se quitan: a simple vista la ruta es igual. */
private const val SIMPLIFY_TOLERANCE_PX = 1.5f

private val OsmHeaders = NetworkHeaders.Builder().set("User-Agent", "GasolineProject/1.0").build()

/**
 * Mapa de la ruta (estilo Strava) sin API key:
 * - Fondo: teselas de OpenStreetMap descargadas con Coil.
 * - Encima: la ruta y los marcadores de inicio y fin.
 *
 * Ligero aunque la ruta tenga miles de puntos GPS: se pasan a pixeles y se simplifican UNA vez
 * (Douglas-Peucker), el trazo se arma una sola vez (drawWithCache) y la animación solo vuelve a
 * dibujar el Canvas, sin recomponer ni recargar las teselas.
 *
 * @param animate si es true, la ruta "se va pintando" del inicio al fin con un punto que la recorre
 *   (una sola vez por tarjeta, aunque se haga scroll y se regrese).
 * @param fitBottomInset alto (desde abajo) que queda tapado (ej. la hoja de detalles): el zoom se
 *   elige para que la ruta quepa en lo que sí se ve.
 * @param contentOffsetY desplazamiento vertical del mapa en px, leído en cada cuadro (ej. para
 *   centrar la ruta en lo visible mientras se arrastra una hoja). Negativo = sube.
 */
@Composable
fun RouteMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    routeColor: Color = AppTheme.colors.primary,
    markerColor: Color = AppTheme.colors.danger,
    animate: Boolean = false,
    animationMillis: Int = 1600,
    fitBottomInset: Dp = 0.dp,
    contentOffsetY: (() -> Float)? = null,
) {
    val colors = AppTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .background(colors.labelBackground),
    ) {
        if (points.isEmpty()) {
            Icon(
                AppIcons.Car,
                contentDescription = null,
                tint = colors.onLabel.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.Center).size(40.dp),
            )
            return@BoxWithConstraints
        }

        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toDouble()
        val heightPx = constraints.maxHeight.toDouble()
        val insetPx = with(density) { fitBottomInset.toPx().toDouble() }.coerceIn(0.0, heightPx * 0.8)
        val viewport = remember(points, widthPx, heightPx, insetPx) {
            Viewport.fit(points, widthPx, heightPx, fitHeight = heightPx - insetPx)
        }
        // Ruta en pixeles ya simplificada: se calcula una vez por tamaño/zoom, no en cada cuadro
        val screenPoints = remember(viewport, points) {
            simplify(points.map(viewport::toScreen), SIMPLIFY_TOLERANCE_PX)
        }

        // ---- Animación: 0 = nada pintado, 1 = ruta completa ----
        var played by rememberSaveable(points.size, points.firstOrNull(), points.lastOrNull()) { mutableStateOf(!animate) }
        val progress = remember { Animatable(if (played) 1f else 0f) }
        LaunchedEffect(played) {
            if (!played) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(durationMillis = animationMillis, easing = FastOutSlowInEasing))
                played = true
            }
        }

        val shiftModifier = if (contentOffsetY != null) {
            Modifier.graphicsLayer { translationY = contentOffsetY() }
        } else {
            Modifier
        }
        Box(Modifier.fillMaxSize().then(shiftModifier)) {
            // Si el mapa se va a desplazar, se cubre el alto tapado de más (arriba y abajo)
            MapTiles(viewport, widthPx, heightPx, extraPx = if (contentOffsetY != null) insetPx else 0.0)

            // Ruta y marcadores (solo se redibuja el Canvas cuando cambia el avance)
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val path = Path().apply {
                            moveTo(screenPoints.first().x, screenPoints.first().y)
                            for (i in 1 until screenPoints.size) lineTo(screenPoints[i].x, screenPoints[i].y)
                        }
                        val measure = PathMeasure().apply { setPath(path, false) }
                        val length = measure.length
                        val partial = Path()
                        val outline = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val line = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val r = 5.dp.toPx()
                        val ring = 2.dp.toPx()
                        onDrawBehind {
                            val t = progress.value
                            val visible = if (t >= 1f || length <= 0f) {
                                path
                            } else {
                                partial.reset()
                                measure.getSegment(0f, length * t, partial, true)
                                partial
                            }
                            if (screenPoints.size > 1) {
                                drawPath(visible, Color.White, style = outline)
                                drawPath(visible, routeColor, style = line)
                            }
                            // Inicio siempre; el fin aparece cuando la ruta llega
                            val start = screenPoints.first()
                            drawCircle(Color.White, radius = r + ring, center = start)
                            drawCircle(markerColor, radius = r, center = start)
                            if (t >= 1f || length <= 0f) {
                                val end = screenPoints.last()
                                drawCircle(Color.White, radius = r + ring, center = end)
                                drawCircle(markerColor, radius = r, center = end)
                            } else {
                                // Punto que va "manejando" por la ruta
                                val head = measure.getPosition(length * t)
                                drawCircle(Color.White, radius = r + ring, center = head)
                                drawCircle(routeColor, radius = r, center = head)
                            }
                        }
                    },
            )
        }

        Text(
            text = "© OpenStreetMap",
            fontSize = 9.sp,
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.White.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

/** Teselas de OSM que cubren el área (las peticiones se recuerdan; Coil las guarda en caché). */
@Composable
private fun MapTiles(viewport: Viewport, widthPx: Double, heightPx: Double, extraPx: Double) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    val tileDp = with(density) { TILE_SIZE.toFloat().toDp() }
    val tiles = remember(viewport, widthPx, heightPx, extraPx) { viewport.visibleTiles(widthPx, heightPx, extraPx) }
    tiles.forEach { tile ->
        val request = remember(tile.url) {
            ImageRequest.Builder(context)
                .data(tile.url)
                // Política de OpenStreetMap: identificar la app
                .httpHeaders(OsmHeaders)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .size(tileDp)
                .offset { IntOffset(tile.offsetX, tile.offsetY) },
        )
    }
}

private class Tile(val url: String, val offsetX: Int, val offsetY: Int)

/** Proyección Web Mercator (la de OpenStreetMap/Google). */
private class Viewport(val zoom: Int, val left: Double, val top: Double) {

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

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val lengthSq = dx * dx + dy * dy
    if (lengthSq == 0f) return sqrt((p.x - a.x) * (p.x - a.x) + (p.y - a.y) * (p.y - a.y))
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSq).coerceIn(0f, 1f)
    val projX = a.x + t * dx
    val projY = a.y + t * dy
    return abs(sqrt((p.x - projX) * (p.x - projX) + (p.y - projY) * (p.y - projY)))
}
