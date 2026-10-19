package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Imagen remota/local con placeholder.
 * model puede ser una URL (http...), una ruta "file://..." o un ByteArray.
 * Si es null o falla, muestra el ícono de respaldo sobre fondo gris.
 */
@Composable
fun AppImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackIcon: ImageVector = AppIcons.Car,
) {
    if (model == null) {
        ImagePlaceholder(modifier, fallbackIcon)
        return
    }
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = { ImagePlaceholder(Modifier.fillMaxSize(), fallbackIcon) },
        error = { ImagePlaceholder(Modifier.fillMaxSize(), fallbackIcon) },
    )
}

@Composable
private fun ImagePlaceholder(modifier: Modifier, icon: ImageVector) {
    Box(modifier.background(AppTheme.colors.labelBackground), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = AppTheme.colors.onLabel.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
    }
}
