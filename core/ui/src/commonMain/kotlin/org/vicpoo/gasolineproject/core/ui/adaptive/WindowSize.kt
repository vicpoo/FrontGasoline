package org.vicpoo.gasolineproject.core.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Clase de ancho de ventana (mismos cortes que Material 3):
 * Compact < 600dp (teléfono) · Medium < 840dp (tablet vertical / plegable / teléfono acostado) · Expanded (tablet horizontal).
 */
enum class WindowWidth { Compact, Medium, Expanded }

/**
 * Clase de alto de ventana (mismos cortes que Material 3):
 * Compact < 480dp (teléfono acostado) · Medium < 900dp · Expanded.
 */
enum class WindowHeight { Compact, Medium, Expanded }

val LocalWindowWidth = staticCompositionLocalOf { WindowWidth.Compact }
val LocalWindowHeight = staticCompositionLocalOf { WindowHeight.Medium }

/**
 * Calcula el tamaño disponible y lo expone a toda la UI vía LocalWindowWidth / LocalWindowHeight.
 * También se usa dentro de cada panel de [ListDetailLayout]: así una pantalla de detalle que
 * vive en medio panel se dibuja con el diseño que corresponde a SU ancho, no al de la tablet.
 */
@Composable
fun ProvideWindowWidth(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = when {
            maxWidth < 600.dp -> WindowWidth.Compact
            maxWidth < 840.dp -> WindowWidth.Medium
            else -> WindowWidth.Expanded
        }
        val height = when {
            maxHeight < 480.dp -> WindowHeight.Compact
            maxHeight < 900.dp -> WindowHeight.Medium
            else -> WindowHeight.Expanded
        }
        CompositionLocalProvider(
            LocalWindowWidth provides width,
            LocalWindowHeight provides height,
            content = content,
        )
    }
}

/** Teléfono acostado (poca altura): conviene poner contenido lado a lado en vez de apilado. */
val isLandscapePhone: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalWindowHeight.current == WindowHeight.Compact

/** Cabe una lista y su detalle lado a lado (tablet horizontal / pantalla grande). */
val showsListAndDetail: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalWindowWidth.current == WindowWidth.Expanded
