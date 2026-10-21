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
import androidx.compose.runtime.remember
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Imagen remota/local con placeholder.
 * model puede ser una URL (http...), una ruta "file://..." o un ByteArray.
 * Si es null o falla, muestra el ícono de respaldo sobre fondo gris.
 *
 * Las fotos de MinIO llegan como URL prefirmada (con ?X-Amz-...): la firma cambia en cada
 * consulta y vence en 1 hora. Por eso el caché de imágenes usa la URL SIN la firma: la foto se
 * descarga una vez y se sigue viendo aunque la URL guardada ya haya vencido (offline-first).
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
    val context = LocalPlatformContext.current
    val request = remember(model, context) {
        if (model is String && model.startsWith("http")) {
            val stableKey = model.substringBefore('?')
            ImageRequest.Builder(context)
                .data(model)
                .memoryCacheKey(stableKey)
                .diskCacheKey(stableKey)
                .build()
        } else {
            model
        }
    }
    SubcomposeAsyncImage(
        model = request,
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
