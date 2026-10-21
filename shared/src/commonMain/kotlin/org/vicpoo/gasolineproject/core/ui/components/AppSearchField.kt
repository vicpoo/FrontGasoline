package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Buscador estilo Airbnb ("Comienza a explorar"): píldora ancha y alta, fondo blanco con sombra,
 * lupa y texto centrados mientras está vacío. Al escribir, el texto va a la izquierda y aparece
 * la X para borrar. Reutilizable en cualquier lista.
 */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    clearDescription: String? = null,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val isEmpty = value.isEmpty()

    // En modo oscuro la sombra no se ve: ahí el buscador se distingue por el fondo y el borde
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(50),
        color = if (colors.isDark) colors.text.copy(alpha = 0.10f) else colors.background,
        border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.2f)),
        shadowElevation = if (colors.isDark) 0.dp else 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimens.spaceL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = AppTheme.typography.body.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerField ->
                        // Vacío: lupa + texto centrados; al escribir, a la izquierda
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isEmpty) Arrangement.Center else Arrangement.Start,
                        ) {
                            Icon(AppIcons.Search, contentDescription = null, tint = colors.title, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(dimens.spaceS))
                            Box(if (isEmpty) Modifier else Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (isEmpty && placeholder.isNotEmpty()) {
                                    // Vacío: lupa y texto en negritas, como "Comienza a explorar"
                                    Text(
                                        text = placeholder,
                                        style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.title,
                                        textAlign = TextAlign.Start,
                                        maxLines = 1,
                                    )
                                }
                                innerField()
                            }
                        }
                    },
                )
            }
            if (!isEmpty) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(32.dp)) {
                    Icon(AppIcons.Close, contentDescription = clearDescription, tint = colors.text, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
