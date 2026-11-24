package org.vicpoo.gasolineproject.ui.common.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import kotlin.math.roundToInt
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.pow

/**
 * Marcador sobre la ruta (ej. dónde se cargó gasolina).
 * @param description texto para accesibilidad (TalkBack / VoiceOver).
 */
data class MapMarker(
    val point: GeoPoint,
    val icon: ImageVector = AppIcons.Fuel,
    val description: String? = null,
)

/** Tamaño del círculo de cada marcador. */
private val MARKER_SIZE = 26.dp

/** Zoom al centrar el mapa en un marcador (calles visibles alrededor de la gasolinera). */
private const val FOCUS_ZOOM = 16

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
    /** Iconos sobre el mapa (ej. cargas de gasolina). Con animación aparecen cuando la ruta pasa por ahí. */
    markers: List<MapMarker> = emptyList(),
    /** Si no es null, el mapa se acerca y se centra en este punto (y su marcador se ve más grande). */
    focusPoint: GeoPoint? = null,
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
        val fitHeight = heightPx - insetPx
        // Toda la ruta. El zoom incluye los marcadores, por si alguno cae un poco fuera de la ruta
        val fitted = remember(points, markers, widthPx, heightPx, insetPx) {
            Viewport.fit(points + markers.map { it.point }, widthPx, heightPx, fitHeight = fitHeight)
        }

        // ---- Acercamiento animado a un marcador (focusPoint) ----
        // zoomProgress: 0 = toda la ruta, 1 = centrado en el marcador. Mientras se mueve, se escala la
        // vista de la ruta completa (las teselas se ven un poco borrosas un instante); al llegar se
        // cambia a las teselas de ese zoom (nítidas). La línea conserva su grosor durante el viaje.
        val zoomProgress = remember { Animatable(0f) }
        // Punto hacia (o desde) el que se anima; null = sin acercamiento
        var animTarget by remember { mutableStateOf<GeoPoint?>(null) }
        // Ya llegó: se dibuja con el viewport del zoom final
        var settled by remember { mutableStateOf<GeoPoint?>(null) }
        // Salto del icono al llegar: 0 = en su lugar, 1 = arriba
        val bounce = remember { Animatable(0f) }
        LaunchedEffect(focusPoint) {
            if (focusPoint != null) {
                // Venía de otra carga: primero se aleja a la ruta completa
                if (animTarget != null && animTarget != focusPoint) {
                    settled = null
                    bounce.snapTo(0f)
                    zoomProgress.animateTo(0f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
                }
                if (settled == focusPoint) return@LaunchedEffect
                animTarget = focusPoint
                zoomProgress.animateTo(1f, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
                settled = focusPoint
                // El icono "salta": sube rápido y cae rebotando
                bounce.animateTo(1f, tween(durationMillis = 180, easing = FastOutSlowInEasing))
                bounce.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMediumLow))
            } else if (animTarget != null) {
                settled = null
                bounce.snapTo(0f)
                zoomProgress.animateTo(0f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
                animTarget = null
            }
        }

        // Viewport que se dibuja: el del zoom final cuando ya llegó; si no, el de toda la ruta
        val viewport = remember(fitted, settled, widthPx, fitHeight) {
            settled?.let { Viewport.centeredOn(it, maxOf(fitted.zoom, FOCUS_ZOOM), widthPx, fitHeight) } ?: fitted
        }
        val center = Offset((widthPx / 2).toFloat(), (fitHeight / 2).toFloat())

        /**
         * Transformación del cuadro actual mientras se acerca/aleja (null = ninguna):
         * un punto p de la vista completa se dibuja en (p - focus) * scale + center.
         */
        fun zoomTransform(): ZoomTransform? {
            val target = animTarget ?: return null
            if (settled != null) return null
            val t = zoomProgress.value
            if (t <= 0f) return null
            val zoomDelta = (maxOf(fitted.zoom, FOCUS_ZOOM) - fitted.zoom).toFloat()
            val scale = 2f.pow(zoomDelta * t)
            val targetPx = fitted.toScreen(target)
            val focus = center + (targetPx - center) * t
            return ZoomTransform(scale, focus, center)
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

        // Cada marcador en pixeles y en qué parte de la ruta (0..1) está, para que aparezca
        // cuando la animación llega a ese punto
        val markerPositions = remember(viewport, markers, screenPoints) {
            val cumulative = cumulativeLengths(screenPoints)
            val total = cumulative.lastOrNull() ?: 0f
            markers.map { marker ->
                val position = viewport.toScreen(marker.point)
                val nearest = screenPoints.indices.minByOrNull { (screenPoints[it] - position).getDistanceSquared() } ?: 0
                val fraction = if (total > 0f) cumulative[nearest] / total else 0f
                Triple(marker, position, fraction)
            }
        }

        val shiftModifier = if (contentOffsetY != null) {
            Modifier.graphicsLayer { translationY = contentOffsetY() }
        } else {
            Modifier
        }
        Box(Modifier.fillMaxSize().then(shiftModifier)) {
            // Si el mapa se va a desplazar, se cubre el alto tapado de más (arriba y abajo)
            Box(
                Modifier
                    .fillMaxSize()
                    // Durante el acercamiento las teselas se escalan (sin volver a pedirlas)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        val zoom = zoomTransform()
                        scaleX = zoom?.scale ?: 1f
                        scaleY = zoom?.scale ?: 1f
                        translationX = zoom?.translation?.x ?: 0f
                        translationY = zoom?.translation?.y ?: 0f
                    },
            ) {
                MapTiles(viewport, widthPx, heightPx, extraPx = if (contentOffsetY != null) insetPx else 0.0)
            }

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
                        val outlinePx = 7.dp.toPx()
                        val linePx = 4.dp.toPx()
                        val rPx = 5.dp.toPx()
                        val ringPx = 2.dp.toPx()
                        onDrawBehind {
                            val t = progress.value
                            val visible = if (t >= 1f || length <= 0f) {
                                path
                            } else {
                                partial.reset()
                                measure.getSegment(0f, length * t, partial, true)
                                partial
                            }
                            // Con acercamiento: se escala el dibujo pero el grosor se divide entre la escala
                            val zoom = zoomTransform()
                            val k = zoom?.scale ?: 1f
                            val outline = Stroke(width = outlinePx / k, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            val line = Stroke(width = linePx / k, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            val r = rPx / k
                            val ring = ringPx / k
                            withTransform({
                                if (zoom != null) {
                                    translate(zoom.translation.x, zoom.translation.y)
                                    scale(zoom.scale, zoom.scale, pivot = Offset.Zero)
                                }
                            }) {
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
                        }
                    },
            )

            // Marcadores (ej. cargas de gasolina): círculo azul con borde blanco y el icono encima
            val fuelMarkerColor = colors.primary
            val markerHalfPx = with(density) { (MARKER_SIZE / 2).roundToPx() }
            val jumpPx = with(density) { 14.dp.toPx() }
            markerPositions.forEach { (marker, position, fraction) ->
                val isFocused = marker.point == focusPoint
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        // Se lee la animación aquí (no al componer): solo se recoloca, sin recomponer
                        .offset {
                            val p = zoomTransform()?.apply(position) ?: position
                            IntOffset(p.x.roundToInt() - markerHalfPx, p.y.roundToInt() - markerHalfPx)
                        }
                        .size(MARKER_SIZE)
                        .graphicsLayer {
                            val shown = progress.value >= fraction || progress.value >= 1f
                            alpha = if (shown) 1f else 0f
                            val jump = if (isFocused) bounce.value else 0f
                            // El enfocado crece al acercarse y "salta" al llegar
                            val grow = if (isFocused) 1f + 0.3f * zoomProgress.value else 1f
                            val scale = if (shown) grow * (1f + 0.25f * jump) else 0.6f
                            scaleX = scale
                            scaleY = scale
                            translationY = -jumpPx * jump
                        }
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(fuelMarkerColor)
                        .border(2.dp, Color.White, CircleShape),
                ) {
                    Icon(
                        imageVector = marker.icon,
                        contentDescription = marker.description,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
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
