package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

private const val TILE_SIZE = 256.0
private const val MAX_ZOOM = 17

/**
 * Mapa de la ruta (estilo Strava) sin API key:
 * - Fondo: teselas de OpenStreetMap descargadas con Coil.
 * - Encima: la ruta dibujada y marcadores de inicio y fin.
 * Reutilizable en el historial, en el detalle del viaje y en el viaje en curso.
 */
@Composable
fun RouteMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    routeColor: Color = AppTheme.colors.primary,
    markerColor: Color = AppTheme.colors.danger,
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
        val viewport = remember(points, widthPx, heightPx) { Viewport.fit(points, widthPx, heightPx) }

        // Teselas que cubren el área visible
        val context = LocalPlatformContext.current
        val tileCount = 1 shl viewport.zoom
        val firstX = floor(viewport.left / TILE_SIZE).toInt()
        val lastX = floor((viewport.left + widthPx) / TILE_SIZE).toInt()
        val firstY = floor(viewport.top / TILE_SIZE).toInt()
        val lastY = floor((viewport.top + heightPx) / TILE_SIZE).toInt()
        val tileDp = with(density) { TILE_SIZE.toFloat().toDp() }

        for (tx in firstX..lastX) {
            for (ty in firstY..lastY) {
                if (ty < 0 || ty >= tileCount) continue
                val wrappedX = ((tx % tileCount) + tileCount) % tileCount
                val url = "https://tile.openstreetmap.org/${viewport.zoom}/$wrappedX/$ty.png"
                val request = ImageRequest.Builder(context)
                    .data(url)
                    // Política de OpenStreetMap: identificar la app
                    .httpHeaders(NetworkHeaders.Builder().set("User-Agent", "GasolineProject/1.0").build())
                    .build()
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .size(tileDp)
                        .offset {
                            IntOffset(
                                (tx * TILE_SIZE - viewport.left).roundToInt(),
                                (ty * TILE_SIZE - viewport.top).roundToInt(),
                            )
                        },
                )
            }
        }

        // Ruta y marcadores
        Canvas(Modifier.fillMaxSize()) {
            val screen = points.map { viewport.toScreen(it) }
            if (screen.size > 1) {
                val path = Path().apply {
                    moveTo(screen.first().x, screen.first().y)
                    screen.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, Color.White, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, routeColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            val r = 5.dp.toPx()
            listOf(screen.first(), screen.last()).forEach { p ->
                drawCircle(Color.White, radius = r + 2.dp.toPx(), center = p)
                drawCircle(markerColor, radius = r, center = p)
            }
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

/** Proyección Web Mercator (la de OpenStreetMap/Google). */
private class Viewport(val zoom: Int, val left: Double, val top: Double) {

    fun toScreen(p: GeoPoint): Offset {
        val (x, y) = project(p, zoom)
        return Offset((x - left).toFloat(), (y - top).toFloat())
    }

    companion object {
        fun fit(points: List<GeoPoint>, width: Double, height: Double): Viewport {
            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLng = points.minOf { it.lng }
            val maxLng = points.maxOf { it.lng }

            // Zoom más grande que aún deja ver toda la ruta con margen
            val padding = 0.75
            var zoom = MAX_ZOOM
            while (zoom > 2) {
                val (x1, y1) = project(GeoPoint(maxLat, minLng), zoom)
                val (x2, y2) = project(GeoPoint(minLat, maxLng), zoom)
                if ((x2 - x1) <= width * padding && (y2 - y1) <= height * padding) break
                zoom--
            }
            val (cx, cy) = project(GeoPoint((minLat + maxLat) / 2, (minLng + maxLng) / 2), zoom)
            return Viewport(zoom, cx - width / 2, cy - height / 2)
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
