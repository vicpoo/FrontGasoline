package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Contenedor base de cada pantalla.
 * - Aplica fondo del tema y márgenes seguros (barra de estado, teclado, notch).
 * - Adaptativo: en tablets centra el contenido y limita su ancho.
 * - Con scroll, pero ocupando al menos toda la altura, así Spacer(Modifier.weight(1f))
 *   sigue funcionando para empujar botones hacia abajo.
 */
@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = AppTheme.dimens.maxContentWidth,
    scrollable: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = AppTheme.dimens
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxContentWidth)
                .then(scrollModifier)
                .heightIn(min = maxHeight)
                .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceM),
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}
