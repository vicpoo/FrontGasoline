package org.vicpoo.gasolineproject.core.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.ui.components.RouteMap

/** iOS: mientras no se agregue el framework de MapLibre (CocoaPods/SPM) se usa el mapa OSM ligero. */
@Composable
actual fun LiveRouteMap(
    route: List<GeoPoint>,
    current: GeoPoint?,
    modifier: Modifier,
    followCurrent: Boolean,
) {
    val points = if (current != null && route.lastOrNull() != current) route + current else route
    RouteMap(points = points, modifier = modifier)
}
