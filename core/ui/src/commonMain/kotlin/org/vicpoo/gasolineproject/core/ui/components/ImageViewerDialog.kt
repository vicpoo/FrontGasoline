package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Visor de imagen a pantalla completa (ej. "Ver recibo"). */
@Composable
fun ImageViewerDialog(
    model: Any?,
    onDismiss: () -> Unit,
    closeDescription: String? = null,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).safeDrawingPadding()) {
            AppImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(AppTheme.dimens.spaceM),
                contentScale = ContentScale.Fit,
                fallbackIcon = AppIcons.FileDocument,
            )
            Surface(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(AppTheme.dimens.spaceM).size(40.dp),
                shape = CircleShape,
                color = Color.White,
                contentColor = Color.Black,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Close, contentDescription = closeDescription, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
