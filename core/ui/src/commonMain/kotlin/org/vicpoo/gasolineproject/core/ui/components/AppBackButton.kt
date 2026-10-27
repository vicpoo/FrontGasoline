package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.resources.Res
import org.vicpoo.gasolineproject.core.ui.resources.cd_back
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Botón circular gris de regresar (esquina superior izquierda en Figma). */
@Composable
fun AppBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Surface(
        onClick = onClick,
        modifier = modifier.size(dimens.iconButtonSize),
        shape = CircleShape,
        color = colors.labelBackground,
        contentColor = colors.onLabel,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = AppIcons.ChevronLeft,
                contentDescription = stringResource(Res.string.cd_back),
                modifier = Modifier.size(dimens.iconSize),
            )
        }
    }
}
