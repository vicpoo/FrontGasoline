package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Aviso de modo sin conexión / cambios pendientes de sincronizar. */
@Composable
fun OfflineBanner(text: String, modifier: Modifier = Modifier) {
    val dimens = AppTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.labelBackground, RoundedCornerShape(dimens.radiusS))
            .padding(horizontal = dimens.spaceM, vertical = dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.CloudOff, contentDescription = null, tint = AppTheme.colors.onLabel, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(dimens.spaceS))
        Text(text, style = AppTheme.typography.caption, color = AppTheme.colors.onLabel)
    }
}
