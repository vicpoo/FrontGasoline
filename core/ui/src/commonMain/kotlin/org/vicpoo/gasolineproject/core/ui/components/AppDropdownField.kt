package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Una opción de AppDropdownField. */
data class DropdownOption(val id: String, val label: String)

/**
 * Lista desplegable con el mismo estilo que AppTextField (borde del color principal y label
 * flotante; rojo con error). Reutilizable: centros de costo, estados, filtros...
 * Si no se manda [label], el [placeholder] hace de label.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    val canOpen = enabled && options.isNotEmpty()

    val supportingContent: (@Composable () -> Unit)? = if (errorMessage != null) {
        { Text(errorMessage) }
    } else {
        null
    }

    ExposedDropdownMenuBox(
        expanded = expanded && canOpen,
        onExpandedChange = { if (canOpen) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.label.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            isError = errorMessage != null,
            textStyle = AppTheme.typography.body,
            label = { Text(label ?: placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = supportingContent,
            shape = RoundedCornerShape(AppTheme.dimens.radiusS),
            colors = appOutlinedFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = canOpen),
        )
        ExposedDropdownMenu(
            expanded = expanded && canOpen,
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
}
