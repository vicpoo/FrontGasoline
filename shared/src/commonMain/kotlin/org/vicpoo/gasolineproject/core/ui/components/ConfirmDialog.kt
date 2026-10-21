package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Qué tan oscuro queda el fondo detrás de la alerta. */
private const val SCRIM_ALPHA = 0.6f

/**
 * Confirmación (Figma: "¿Está seguro de eliminar este vehículo?") como hoja que sube desde abajo,
 * estilo Airbnb: oscurece TODA la pantalla (también la barra de estado y la de navegación) y la
 * alerta queda al alcance del pulgar.
 * - [icon] opcional arriba (ej. bote de basura al eliminar), en un círculo del color de [iconTint].
 * - "Sí" = botón de confirmación (azul), "No" = botón de cancelar (rojo), uno debajo del otro.
 * - Mientras [isLoading] no se puede cerrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    iconTint: Color = AppTheme.colors.danger,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val loading by rememberUpdatedState(isLoading)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value -> value != SheetValue.Hidden || !loading },
    )

    ModalBottomSheet(
        onDismissRequest = { if (!loading) onDismiss() },
        sheetState = sheetState,
        containerColor = colors.background,
        scrimColor = Color.Black.copy(alpha = SCRIM_ALPHA),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = dimens.spaceL, end = dimens.spaceL, bottom = dimens.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (icon != null) {
                    Surface(shape = CircleShape, color = iconTint.copy(alpha = 0.12f), modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(Modifier.height(dimens.spaceM))
                }
                SubtitleText(text = message, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(dimens.spaceL))
                ConfirmButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading,
                )
                Spacer(Modifier.height(dimens.spaceS))
                DangerButton(
                    text = dismissText,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                )
            }
        }
    }
}
