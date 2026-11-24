package org.vicpoo.gasolineproject.ui.common.components

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade

/* Mosaicos de OpenStreetMap del mapa de ruta (separados de RouteMap, punto 3.2.3). */

internal const val TILE_SIZE = 256.0

internal const val MAX_ZOOM = 17

internal val OsmHeaders = NetworkHeaders.Builder().set("User-Agent", "GasolineProject/1.0").build()

/** Teselas de OSM que cubren el área (las peticiones se recuerdan; Coil las guarda en caché). */
@Composable
internal fun MapTiles(viewport: Viewport, widthPx: Double, heightPx: Double, extraPx: Double) {
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

internal class Tile(val url: String, val offsetX: Int, val offsetY: Int)
