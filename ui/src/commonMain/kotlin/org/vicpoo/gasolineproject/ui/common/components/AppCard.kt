package org.vicpoo.gasolineproject.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme

/** Tarjeta sin borde: se separa del fondo por contraste (colors.card). Si onClick != null es presionable. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    /** null = el fondo normal de tarjeta (colors.card); ej. azul tenue para marcar la elegida. */
    containerColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val shape = RoundedCornerShape(dimens.radius)
    val background = containerColor ?: colors.card

    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = background) {
            Box(Modifier.padding(dimens.spaceS), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = background) {
            Box(Modifier.padding(dimens.spaceS), content = content)
        }
    }
}
