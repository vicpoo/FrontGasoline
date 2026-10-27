package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Píldora de estado (ej. "Disponible"). El color lo decide quien la usa, siempre desde el tema. */
@Composable
fun StatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .background(containerColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 2.dp),
        style = AppTheme.typography.caption,
        color = contentColor,
    )
}
