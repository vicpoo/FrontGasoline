package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.resources.Res
import org.vicpoo.gasolineproject.core.ui.resources.cd_hide_password
import org.vicpoo.gasolineproject.core.ui.resources.cd_show_password
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Campo de texto de TODA la app (componente reutilizable) con label flotante:
 * - El label ("Marca") va dentro del campo; al tocarlo (o cuando ya tiene texto) sube al borde.
 * - Borde del color principal; al enfocar se hace más grueso y el label también es del color principal.
 * - Con error: borde, label y mensaje en rojo.
 * - isPassword = true: oculta el texto y agrega el botón de ver/ocultar.
 * - suffix: unidad al final del campo (ej. "km", "L").
 * - helperText: ayuda debajo del campo cuando no hay error.
 * - placeholder: ejemplo que aparece al enfocar el campo vacío (ej. "ej. Nissan").
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isPassword: Boolean = false,
    errorMessage: String? = null,
    helperText: String? = null,
    suffix: String? = null,
    enabled: Boolean = true,
    /** false + minLines > 1 = área de texto (ej. "Motivo de la solicitud"). */
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    // Tipos explícitos: los slots de Material3 piden lambdas @Composable
    val placeholderContent: (@Composable () -> Unit)? = if (placeholder != null) {
        { Text(placeholder, style = AppTheme.typography.body) }
    } else {
        null
    }
    val suffixContent: (@Composable () -> Unit)? = if (suffix != null) {
        { Text(suffix, style = AppTheme.typography.body) }
    } else {
        null
    }
    val supportingContent: (@Composable () -> Unit)? = when {
        errorMessage != null -> { { Text(errorMessage) } }
        helperText != null -> { { Text(helperText) } }
        else -> null
    }
    val trailingContent: (@Composable () -> Unit)? = if (isPassword) {
        {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) AppIcons.EyeOff else AppIcons.Eye,
                    contentDescription = stringResource(
                        if (passwordVisible) Res.string.cd_hide_password else Res.string.cd_show_password,
                    ),
                )
            }
        }
    } else {
        null
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        isError = errorMessage != null,
        textStyle = AppTheme.typography.body,
        label = { Text(label) },
        placeholder = placeholderContent,
        suffix = suffixContent,
        trailingIcon = trailingContent,
        supportingText = supportingContent,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(AppTheme.dimens.radiusS),
        colors = appOutlinedFieldColors(),
    )
}

/** Colores compartidos por AppTextField y AppDropdownField (mismo look en toda la app). */
@Composable
internal fun appOutlinedFieldColors(): TextFieldColors {
    val colors = AppTheme.colors
    // El campo ya no tiene caja gris: el texto usa el color normal de la pantalla
    // (oscuro en modo claro, claro en modo oscuro) para que siempre se lea
    val placeholder = colors.textSecondary.copy(alpha = 0.7f)
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.text,
        unfocusedTextColor = colors.text,
        disabledTextColor = colors.text.copy(alpha = 0.6f),
        errorTextColor = colors.text,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        cursorColor = colors.primary,
        errorCursorColor = colors.error,
        // Borde del color principal (más grueso al enfocar); rojo con error
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.primary.copy(alpha = 0.6f),
        disabledBorderColor = colors.primary.copy(alpha = 0.3f),
        errorBorderColor = colors.error,
        // Label: dentro del campo en gris; arriba en el color principal al enfocar; rojo con error
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.textSecondary,
        disabledLabelColor = colors.textSecondary.copy(alpha = 0.6f),
        errorLabelColor = colors.error,
        focusedPlaceholderColor = placeholder,
        unfocusedPlaceholderColor = placeholder,
        errorPlaceholderColor = placeholder,
        focusedTrailingIconColor = colors.text,
        unfocusedTrailingIconColor = colors.text,
        errorTrailingIconColor = colors.error,
        focusedSuffixColor = colors.text,
        unfocusedSuffixColor = colors.text,
        errorSuffixColor = colors.error,
        focusedSupportingTextColor = colors.textSecondary,
        unfocusedSupportingTextColor = colors.textSecondary,
        errorSupportingTextColor = colors.error,
    )
}
