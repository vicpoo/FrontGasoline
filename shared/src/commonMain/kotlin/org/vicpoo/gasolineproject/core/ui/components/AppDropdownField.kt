package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Una opción de AppDropdownField. */
data class DropdownOption(val id: String, val label: String)

/**
 * Lista desplegable con el mismo estilo que AppTextField (caja gris, label arriba).
 * Reutilizable: centros de costo, estados, filtros...
 */
@Composable
fun AppDropdownField(
    options: List<DropdownOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }

    Column(modifier) {
        if (label != null) {
            Text(text = label, style = AppTheme.typography.body, color = colors.subtitle)
            Spacer(Modifier.height(dimens.spaceS))
        }
        Box {
            Surface(
                onClick = { expanded = true },
                enabled = enabled && options.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.buttonHeight),
                shape = RoundedCornerShape(dimens.radiusS),
                color = colors.inputBackground,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = dimens.spaceM, vertical = dimens.spaceS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selected?.label ?: placeholder,
                        style = AppTheme.typography.body,
                        color = if (selected != null) colors.onInput else colors.inputPlaceholder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(AppIcons.ChevronDown, contentDescription = null, tint = colors.onInput)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = colors.background,
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, style = AppTheme.typography.body, color = colors.text) },
                        onClick = {
                            expanded = false
                            onSelect(option.id)
                        },
                    )
                }
            }
        }
        if (errorMessage != null) {
            Spacer(Modifier.height(dimens.spaceXs))
            ErrorText(errorMessage)
        }
    }
}
