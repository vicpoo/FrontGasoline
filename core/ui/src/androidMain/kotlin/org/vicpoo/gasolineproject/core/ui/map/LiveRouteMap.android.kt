package org.vicpoo.gasolineproject.core.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import kotlin.time.Duration.Companion.milliseconds

@Composable
actual fun LiveRouteMap(
    route: List<GeoPoint>,
    current: GeoPoint?,
    modifier: Modifier,
    followCurrent: Boolean,
) {
    val colors = AppTheme.colors
    val start = current ?: route.lastOrNull()
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = (start ?: MapStyles.DEFAULT_CENTER).toPosition(),
            zoom = if (start != null) MapStyles.FOLLOW_ZOOM else MapStyles.DEFAULT_ZOOM,
        ),
    )

    // Sin seguir al conductor (ej. "Registrar Viaje"): se encuadra toda la ruta
    LaunchedEffect(route, followCurrent) {
        if (!followCurrent && route.size >= 2) {
            val (center, zoom) = RouteFrame.of(route)
            cameraState.animateTo(
                finalPosition = cameraState.position.copy(target = center.toPosition(), zoom = zoom),
                duration = 400.milliseconds,
            )
        }
    }

    // Sigue al conductor cada vez que llega una nueva ubicación
    LaunchedEffect(current, followCurrent) {
        if (followCurrent && current != null) {
            cameraState.animateTo(
                finalPosition = cameraState.position.copy(
                    target = current.toPosition(),
                    zoom = maxOf(cameraState.position.zoom, MapStyles.FOLLOW_ZOOM - 1),
                ),
                duration = 600.milliseconds,
            )
        }
    }

    val styleUri = if (colors.isDark) MapStyles.DARK else MapStyles.LIGHT
    val routeJson = remember(route) { GeoJson.lineString(route) }
    val currentJson = remember(current) { GeoJson.point(current) }

    MaplibreMap(
        modifier = modifier,
        baseStyle = BaseStyle.Uri(styleUri),
        cameraState = cameraState,
    ) {
        val routeSource = rememberGeoJsonSource(GeoJsonData.JsonString(routeJson))
        // Contorno blanco + línea del color principal (se ve bien en claro y oscuro)
        LineLayer(
            id = "trip-route-casing",
            source = routeSource,
            color = const(Color.White),
            width = const(8.dp),
            cap = const(LineCap.Round),
            join = const(LineJoin.Round),
        )
        LineLayer(
            id = "trip-route",
            source = routeSource,
            color = const(colors.primary),
            width = const(5.dp),
            cap = const(LineCap.Round),
            join = const(LineJoin.Round),
        )

        val currentSource = rememberGeoJsonSource(GeoJsonData.JsonString(currentJson))
        CircleLayer(
            id = "trip-current",
            source = currentSource,
            color = const(colors.danger),
            radius = const(8.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(3.dp),
        )
    }
}

private fun GeoPoint.toPosition() = Position(longitude = lng, latitude = lat)

/** Centro y zoom aproximado para que toda la ruta quepa en el mapa. */
private object RouteFrame {
    fun of(route: List<GeoPoint>): Pair<GeoPoint, Double> {
        val minLat = route.minOf { it.lat }
        val maxLat = route.maxOf { it.lat }
        val minLng = route.minOf { it.lng }
        val maxLng = route.maxOf { it.lng }
        val center = GeoPoint((minLat + maxLat) / 2, (minLng + maxLng) / 2)
        val span = maxOf(maxLat - minLat, maxLng - minLng).coerceAtLeast(0.0005)
        // 360° caben en zoom 0; cada nivel divide a la mitad. Se resta 1 para dejar margen.
        val zoom = (kotlin.math.ln(360.0 / span) / kotlin.math.ln(2.0) - 1.0).coerceIn(3.0, 17.0)
        return center to zoom
    }
}

/** GeoJSON mínimo armado a mano (evita depender de la API de geometrías de spatialk). */
private object GeoJson {
    private const val EMPTY = """{"type":"FeatureCollection","features":[]}"""

    fun lineString(points: List<GeoPoint>): String {
        if (points.size < 2) return EMPTY
        val coordinates = points.joinToString(separator = ",") { "[${it.lng},${it.lat}]" }
        return """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$coordinates]}}"""
    }

    fun point(point: GeoPoint?): String {
        if (point == null) return EMPTY
        return """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[${point.lng},${point.lat}]}}"""
    }
}
