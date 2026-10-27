package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Botón propio para "agregar" (ej. "Agregar vehículo"): píldora del color principal, centrada
 * abajo de la pantalla, con el "+" en un círculo blanco y el texto. Reutilizable para cualquier alta.
 */
@Composable
fun AppAddButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = AppIcons.Plus,
) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(50),
        color = colors.primary,
        contentColor = colors.onPrimary,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = CircleShape, color = colors.onPrimary, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(text = text, style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}
