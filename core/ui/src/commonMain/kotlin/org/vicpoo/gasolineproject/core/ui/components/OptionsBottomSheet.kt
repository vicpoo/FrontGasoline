package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

data class SheetOption(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * Hoja inferior con opciones (Figma: "Foto de perfil" -> Galería / Cámara).
 * Reutilizable para cualquier menú de acciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsBottomSheet(
    title: String,
    options: List<SheetOption>,
    onDismiss: () -> Unit,
    closeDescription: String? = null,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        contentColor = colors.text,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = dimens.spaceL)) {
            Box(Modifier.fillMaxWidth().padding(horizontal = dimens.spaceS)) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(AppIcons.Close, contentDescription = closeDescription, tint = colors.text)
                }
                Text(
                    text = title,
                    modifier = Modifier.align(Alignment.Center),
                    style = AppTheme.typography.body,
                    color = colors.title,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(dimens.spaceS))
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = option.onClick)
                        .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(option.icon, contentDescription = null, tint = colors.text, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(dimens.spaceM))
                    Text(option.label, style = AppTheme.typography.body, color = colors.text)
                }
            }
        }
    }
}
