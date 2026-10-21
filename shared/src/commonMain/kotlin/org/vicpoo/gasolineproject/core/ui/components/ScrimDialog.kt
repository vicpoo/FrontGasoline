package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Opacidad del fondo detrás de una alerta: la pantalla se oscurece y la alerta sobresale. */
private const val SCRIM_ALPHA = 0.6f

/**
 * Diálogo con el fondo opacado (Figma "¿Está seguro...?").
 * Ocupa toda la pantalla con una capa oscura y centra el contenido; tocar el fondo cierra
 * el diálogo solo si [dismissible] (mientras se guarda no se puede cerrar).
 */
@Composable
fun ScrimDialog(
    onDismissRequest: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val noRipple = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(interactionSource = noRipple, indication = null) { if (dismissible) onDismissRequest() }
                .safeDrawingPadding()
                .padding(AppTheme.dimens.screenPadding),
            contentAlignment = Alignment.Center,
        ) {
            // Los toques sobre la alerta no llegan al fondo (no la cierran)
            Box(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
                content()
            }
        }
    }
}
