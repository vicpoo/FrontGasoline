package org.vicpoo.gasolineproject.ui.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme

/**
 * Lo que se está pidiendo, rechazando o soltando, dentro de una [EditSheet] (como en "Enviar solicitud"):
 * foto del vehículo (o su ícono), nombre, placas y una línea opcional (ej. "Asignado a Ana").
 */
@Composable
fun SheetSubjectCard(
    title: String,
    subtitle: String?,
    imageModel: Any?,
    modifier: Modifier = Modifier,
    fallbackIcon: ImageVector = AppIcons.Car,
    detail: String? = null,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radius),
        color = colors.card,
    ) {
        Row(Modifier.padding(dimens.spaceM), verticalAlignment = Alignment.CenterVertically) {
            AppImage(
                model = imageModel,
                fallbackIcon = fallbackIcon,
                contentDescription = title,
                modifier = Modifier.size(width = 88.dp, height = 72.dp).clip(RoundedCornerShape(dimens.radius)),
            )
            Spacer(Modifier.width(dimens.spaceM))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = AppTheme.typography.bodySecondary, color = colors.textSecondary)
                }
                detail?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = AppTheme.typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Contador "37/255" debajo del campo de texto de una hoja (como en "Enviar solicitud"). */
@Composable
fun SheetCharCounter(length: Int, max: Int, modifier: Modifier = Modifier) {
    Text(
        text = "$length/$max",
        style = AppTheme.typography.caption,
        color = if (length > max) AppTheme.colors.error else AppTheme.colors.textSecondary,
        modifier = modifier,
    )
}
