package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons

/**
 * Foto de perfil redonda de una persona (empleado, operador, confirmador...).
 * Si no tiene foto muestra el ícono de persona. Componente único para toda la app.
 */
@Composable
fun UserAvatar(
    photoUrl: Any?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    contentDescription: String? = null,
) {
    AppImage(
        model = photoUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        fallbackIcon = AppIcons.User,
        modifier = modifier.size(size).clip(CircleShape),
    )
}
