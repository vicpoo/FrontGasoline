package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Foto circular con botón de cámara abajo a la derecha (Figma: perfil). */
@Composable
fun EditableAvatar(
    photoModel: Any?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    isLoading: Boolean = false,
    editDescription: String? = null,
) {
    val colors = AppTheme.colors
    Box(modifier = modifier.size(size)) {
        AppImage(
            model = photoModel,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            fallbackIcon = AppIcons.User,
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = colors.primary,
            )
        }
        Surface(
            onClick = onEditClick,
            enabled = !isLoading,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .size(size * 0.34f),
            shape = CircleShape,
            color = colors.labelBackground,
            contentColor = colors.onLabel,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(AppIcons.Camera, contentDescription = editDescription, modifier = Modifier.size(size * 0.18f))
            }
        }
    }
}
