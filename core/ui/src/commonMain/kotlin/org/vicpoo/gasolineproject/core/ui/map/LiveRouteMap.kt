package org.vicpoo.gasolineproject.core.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.vicpoo.gasolineproject.core.common.GeoPoint

/**
 * Mapa del viaje en curso: dibuja la ruta recorrida y la posición actual, y sigue al usuario.
 * - Android: MapLibre (maplibre-compose) con estilos de OpenFreeMap (sin API key).
 * - iOS: por ahora usa RouteMap (mosaicos OSM) hasta agregar el framework de MapLibre.
 */
@Composable
expect fun LiveRouteMap(
    route: List<GeoPoint>,
    current: GeoPoint?,
    modifier: Modifier = Modifier,
    followCurrent: Boolean = true,
)

/** Estilos de mapa (cámbialos aquí para toda la app). */
object MapStyles {
    const val LIGHT = "https://tiles.openfreemap.org/styles/liberty"
    const val DARK = "https://tiles.openfreemap.org/styles/dark"
    const val FOLLOW_ZOOM = 16.0
    const val DEFAULT_ZOOM = 4.5

    /** Centro de México: se usa mientras todavía no hay señal GPS. */
    val DEFAULT_CENTER = GeoPoint(lat = 19.4326, lng = -99.1332)
}
