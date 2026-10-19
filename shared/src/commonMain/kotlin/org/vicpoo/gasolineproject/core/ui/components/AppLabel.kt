package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Label para identificar o agrupar información (fondo #E0E0E0, texto #1F1F1F). */
@Composable
fun AppLabel(text: String, modifier: Modifier = Modifier) {
    val dimens = AppTheme.dimens
    Text(
        text = text,
        modifier = modifier
            .background(AppTheme.colors.labelBackground, RoundedCornerShape(dimens.radiusS))
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS),
        style = AppTheme.typography.bodySecondary,
        color = AppTheme.colors.onLabel,
    )
}
