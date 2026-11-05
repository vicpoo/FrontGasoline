package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Tarjeta sin borde: se separa del fondo por contraste (colors.card). Si onClick != null es presionable. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val shape = RoundedCornerShape(dimens.radius)

    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = colors.card) {
            Box(Modifier.padding(dimens.spaceS), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = colors.card) {
            Box(Modifier.padding(dimens.spaceS), content = content)
        }
    }
}
