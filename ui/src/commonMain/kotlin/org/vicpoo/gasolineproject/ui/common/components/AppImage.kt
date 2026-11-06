package org.vicpoo.gasolineproject.ui.common.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme

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
    // AsyncImage (no SubcomposeAsyncImage): en listas y grids de carros no crea una subcomposición
    // por imagen. El placeholder va DEBAJO y se quita en cuanto la foto se pinta (o se queda si falla).
    var loaded by remember(request) { mutableStateOf(false) }
    Box(modifier) {
        if (!loaded) ImagePlaceholder(Modifier.matchParentSize(), fallbackIcon)
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale,
            onState = { loaded = it is AsyncImagePainter.State.Success },
        )
    }
}

@Composable
private fun ImagePlaceholder(modifier: Modifier, icon: ImageVector) {
    Box(modifier.background(AppTheme.colors.labelBackground), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = AppTheme.colors.onLabel.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
    }
}
