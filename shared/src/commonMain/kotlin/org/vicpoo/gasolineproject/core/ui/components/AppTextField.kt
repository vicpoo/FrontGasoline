package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.cd_hide_password
import gasolineproject.shared.generated.resources.cd_show_password
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Campo de texto de la app: label arriba y caja gris sin borde (como en Figma).
 * - isPassword = true: oculta el texto y agrega el botón de ver/ocultar.
 * - suffix: unidad al final del campo (ej. "km", "L").
 * - helperText: ayuda debajo del campo cuando no hay error.
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
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    var passwordVisible by remember { mutableStateOf(false) }

    val placeholderContent: (@Composable () -> Unit)? = if (placeholder != null) {
        { Text(placeholder, style = AppTheme.typography.body) }
    } else {
        null
    }
    val suffixContent: (@Composable () -> Unit)? = if (suffix != null) {
        { Text(suffix, style = AppTheme.typography.body, color = colors.onInput) }
    } else {
        null
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

    Column(modifier = modifier) {
        Text(text = label, style = AppTheme.typography.body, color = colors.subtitle)
        Spacer(Modifier.height(dimens.spaceS))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            isError = errorMessage != null,
            textStyle = AppTheme.typography.body,
            placeholder = placeholderContent,
            suffix = suffixContent,
            trailingIcon = trailingContent,
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(dimens.radiusS),
            colors = TextFieldDefaults.colors(
                focusedTextColor = colors.onInput,
                unfocusedTextColor = colors.onInput,
                disabledTextColor = colors.onInput.copy(alpha = 0.6f),
                errorTextColor = colors.onInput,
                focusedContainerColor = colors.inputBackground,
                unfocusedContainerColor = colors.inputBackground,
                disabledContainerColor = colors.inputBackground,
                errorContainerColor = colors.inputBackground,
                cursorColor = colors.primary,
                errorCursorColor = colors.error,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = colors.error,
                focusedTrailingIconColor = colors.onInput,
                unfocusedTrailingIconColor = colors.onInput,
                errorTrailingIconColor = colors.error,
                focusedPlaceholderColor = colors.inputPlaceholder,
                unfocusedPlaceholderColor = colors.inputPlaceholder,
                errorPlaceholderColor = colors.inputPlaceholder,
            ),
        )
        when {
            errorMessage != null -> {
                Spacer(Modifier.height(dimens.spaceXs))
                ErrorText(errorMessage)
            }
            helperText != null -> {
                Spacer(Modifier.height(dimens.spaceXs))
                Text(helperText, style = AppTheme.typography.caption, color = colors.textSecondary)
            }
        }
    }
}
