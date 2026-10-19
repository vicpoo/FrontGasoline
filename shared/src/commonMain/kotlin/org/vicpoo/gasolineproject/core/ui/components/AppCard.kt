package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Tarjeta con borde suave (como las tarjetas de vehículo en Figma). Si onClick != null es presionable. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val shape = RoundedCornerShape(dimens.radiusM)
    val border = BorderStroke(1.dp, colors.divider)

    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = colors.background, border = border) {
            Box(Modifier.padding(dimens.spaceS), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = colors.background, border = border) {
            Box(Modifier.padding(dimens.spaceS), content = content)
        }
    }
}
