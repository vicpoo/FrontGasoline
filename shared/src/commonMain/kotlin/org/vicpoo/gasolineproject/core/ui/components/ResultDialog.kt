package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

enum class ResultType { Success, Error }

/**
 * Diálogo de resultado (Figma: "¡Nombre guardado!" / "¡Error al guardar!").
 * - Success: insignia con ✓ y botón principal (azul) "Entendido".
 * - Error:   insignia con ✕ y botón rojo "Reintentar".
 * Reutilizable en cualquier pantalla que guarde algo.
 */
@Composable
fun ResultDialog(
    type: ResultType,
    title: String,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 400.dp),
            shape = RoundedCornerShape(dimens.radiusM * 2),
            color = colors.background,
        ) {
            Column(
                modifier = Modifier.padding(dimens.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Badge, contentDescription = null, tint = colors.title, modifier = Modifier.size(56.dp))
                    Icon(
                        imageVector = if (type == ResultType.Success) AppIcons.Check else AppIcons.Close,
                        contentDescription = null,
                        tint = colors.title,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(dimens.spaceM))
                Text(title, style = AppTheme.typography.body, color = colors.title, textAlign = TextAlign.Center)
                Spacer(Modifier.height(dimens.spaceXs))
                Text(
                    message,
                    style = AppTheme.typography.caption,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(dimens.spaceL))
                AppButton(
                    text = buttonText,
                    onClick = onButtonClick,
                    modifier = Modifier.fillMaxWidth(),
                    variant = if (type == ResultType.Success) AppButtonVariant.Primary else AppButtonVariant.Danger,
                )
            }
        }
    }
}
