package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Selector de foto reutilizable.
 * - Sin foto: recuadro punteado con cámara ("Toca para agregar una foto").
 * - Con foto: vista previa con botón X para quitarla (como "Editar vehículo" en Figma).
 */
@Composable
fun PhotoPickerBox(
    photoModel: Any?,
    emptyText: String,
    changeText: String,
    removeDescription: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val shape = RoundedCornerShape(dimens.radius)

    if (photoModel == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(colors.labelBackground.copy(alpha = 0.35f))
                .drawBehind {
                    drawRoundRect(
                        color = colors.primary,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)),
                        ),
                        cornerRadius = CornerRadius(dimens.radius.toPx()),
                    )
                }
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(AppIcons.Camera, contentDescription = null, tint = colors.primary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(dimens.spaceS))
                Text(emptyText, style = AppTheme.typography.body, color = colors.text, textAlign = TextAlign.Center)
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(height).clip(shape)) {
                AppImage(model = photoModel, contentDescription = null, modifier = Modifier.fillMaxSize())
                Surface(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).padding(dimens.spaceS).size(32.dp),
                    shape = CircleShape,
                    color = AppTheme.colors.background,
                    contentColor = AppTheme.colors.text,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Close, contentDescription = removeDescription, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(dimens.spaceS))
            AppTextButton(text = changeText, onClick = onPick, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
