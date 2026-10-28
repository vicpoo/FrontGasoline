package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.resources.Res
import org.vicpoo.gasolineproject.core.ui.resources.updated_days_ago
import org.vicpoo.gasolineproject.core.ui.resources.updated_hours_ago
import org.vicpoo.gasolineproject.core.ui.resources.updated_just_now
import org.vicpoo.gasolineproject.core.ui.resources.updated_minutes_ago
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Aviso de modo sin conexión / cambios pendientes de sincronizar.
 * @param lastUpdatedAtMillis si se conoce, agrega "Actualizado hace X" (cuándo se trajo la copia guardada).
 */
@Composable
fun OfflineBanner(text: String, modifier: Modifier = Modifier, lastUpdatedAtMillis: Long? = null) {
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
        Column {
            Text(text, style = AppTheme.typography.caption, color = AppTheme.colors.onLabel)
            lastUpdatedAtMillis?.let {
                Text(updatedAgoText(it), style = AppTheme.typography.caption, color = AppTheme.colors.onLabel.copy(alpha = 0.8f))
            }
        }
    }
}

/** "Actualizado hace un momento / hace 5 min / hace 3 h / hace 2 días". */
@OptIn(ExperimentalTime::class)
@Composable
fun updatedAgoText(epochMillis: Long): String {
    val minutes = ((Clock.System.now().toEpochMilliseconds() - epochMillis) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> stringResource(Res.string.updated_just_now)
        minutes < 60 -> stringResource(Res.string.updated_minutes_ago, minutes.toInt())
        minutes < 24 * 60 -> stringResource(Res.string.updated_hours_ago, (minutes / 60).toInt())
        else -> stringResource(Res.string.updated_days_ago, (minutes / (24 * 60)).toInt())
    }
}
