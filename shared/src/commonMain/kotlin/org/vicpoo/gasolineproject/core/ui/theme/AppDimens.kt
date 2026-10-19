package org.vicpoo.gasolineproject.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Espaciados, radios y tamaños de toda la app. */
@Immutable
data class AppDimens(
    val screenPadding: Dp = 24.dp,
    val spaceXs: Dp = 4.dp,
    val spaceS: Dp = 8.dp,
    val spaceM: Dp = 16.dp,
    val spaceL: Dp = 24.dp,
    val spaceXl: Dp = 32.dp,
    val spaceXxl: Dp = 48.dp,
    val radiusS: Dp = 8.dp,
    val radiusM: Dp = 12.dp,
    val buttonHeight: Dp = 52.dp,
    val iconButtonSize: Dp = 40.dp,
    val iconSize: Dp = 24.dp,
    /** Ancho máximo del contenido en tablets para que los formularios no se estiren. */
    val maxContentWidth: Dp = 480.dp,
    val maxContentWidthWide: Dp = 960.dp,
)
