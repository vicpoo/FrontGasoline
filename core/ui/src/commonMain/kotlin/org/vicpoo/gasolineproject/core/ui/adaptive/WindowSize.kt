package org.vicpoo.gasolineproject.core.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Clase de ancho de ventana (mismos cortes que Material 3):
 * Compact < 600dp (teléfono) · Medium < 840dp (tablet vertical / plegable) · Expanded (tablet horizontal).
 */
enum class WindowWidth { Compact, Medium, Expanded }

val LocalWindowWidth = staticCompositionLocalOf { WindowWidth.Compact }

/** Calcula el ancho disponible y lo expone a toda la UI vía LocalWindowWidth. */
@Composable
fun ProvideWindowWidth(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = when {
            maxWidth < 600.dp -> WindowWidth.Compact
            maxWidth < 840.dp -> WindowWidth.Medium
            else -> WindowWidth.Expanded
        }
        CompositionLocalProvider(LocalWindowWidth provides width, content = content)
    }
}
