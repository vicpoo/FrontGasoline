package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Título de las pantallas principales de cada pestaña (Vehículos, Solicitudes, Registrar,
 * Historial, Perfil). Todas lo usan para que el título quede en la MISMA posición y tamaño:
 * - La pantalla aplica statusBarsPadding (o AppScreen) y el padding horizontal.
 * - Aquí: espacio arriba, fila de 48 dp con el título (y un botón opcional a la derecha,
 *   ej. la campana) y el subtítulo opcional.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** Espacio arriba del título (0 dentro de AppScreen, que ya trae su margen). */
    topSpacing: Dp = AppTheme.dimens.spaceM,
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Spacer(Modifier.height(topSpacing))
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TitleText(title, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        if (subtitle != null) {
            SecondaryText(subtitle)
        }
    }
}
