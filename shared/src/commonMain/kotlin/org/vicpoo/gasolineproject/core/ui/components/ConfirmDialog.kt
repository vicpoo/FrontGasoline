package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Diálogo de confirmación (Figma: "¿Está seguro de eliminar este vehículo?").
 * Según la guía: "Sí" = botón de confirmación (azul), "No" = botón de cancelar (rojo).
 */
@Composable
fun ConfirmDialog(
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
) {
    val dimens = AppTheme.dimens
    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isLoading, dismissOnClickOutside = !isLoading),
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 400.dp),
            shape = RoundedCornerShape(dimens.radiusM * 2),
            color = AppTheme.colors.background,
        ) {
            Column(Modifier.padding(dimens.spaceL)) {
                BodyText(text = message, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(dimens.spaceL))
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceM)) {
                    ConfirmButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        isLoading = isLoading,
                    )
                    DangerButton(
                        text = dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                    )
                }
            }
        }
    }
}
