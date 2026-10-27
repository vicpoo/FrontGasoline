package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Fila "Etiqueta ........ Valor" con línea divisoria (detalle de vehículo en Figma). */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val dimens = AppTheme.dimens
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = AppTheme.typography.bodySecondary, color = AppTheme.colors.textSecondary)
            Spacer(Modifier.width(dimens.spaceM))
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = AppTheme.typography.body,
                color = AppTheme.colors.text,
                textAlign = TextAlign.End,
            )
        }
        if (showDivider) HorizontalDivider(color = AppTheme.colors.divider)
    }
}
