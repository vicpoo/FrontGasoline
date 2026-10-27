package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Tipos de botón definidos en la guía de enmaquetado. */
enum class AppButtonVariant {
    /** Guardar, Continuar, Aceptar, Iniciar sesión, Agregar. */
    Primary,

    /** Confirmar, Sí, Aceptar, Guardar cambios. */
    Confirm,

    /** Cancelar, No, Reintentar, Eliminar, Salir. */
    Danger,

    /** Ver recibo, Ver detalles, Mantenimiento, Consultar, Administrar. */
    Secondary,
}

/**
 * Botón base de la app. Todas las pantallas usan este componente
 * (o sus atajos de abajo); los colores salen del tema según la variante.
 * outlined = true dibuja solo el borde y el texto con el color de la variante.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    outlined: Boolean = false,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val container = variant.containerColor()
    val content = variant.contentColor()
    val shape = RoundedCornerShape(dimens.radiusM)
    val padding = PaddingValues(horizontal = dimens.spaceM, vertical = dimens.spaceS)
    val buttonModifier = modifier.heightIn(min = dimens.buttonHeight)

    if (outlined) {
        val outlineColor = if (variant == AppButtonVariant.Secondary && colors.isDark) colors.text else container
        OutlinedButton(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = buttonModifier,
            shape = shape,
            border = BorderStroke(1.dp, outlineColor),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = outlineColor,
                disabledContentColor = outlineColor.copy(alpha = 0.5f),
            ),
            contentPadding = padding,
        ) {
            ButtonContent(text = text, isLoading = isLoading, leadingIcon = leadingIcon, contentColor = outlineColor)
        }
    } else {
        // Botón de mantenimiento en modo oscuro: borde claro para que no se pierda sobre el fondo negro
        val border = if (variant == AppButtonVariant.Secondary && colors.secondaryBorder != Color.Transparent) {
            BorderStroke(1.dp, colors.secondaryBorder)
        } else {
            null
        }
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = buttonModifier,
            shape = shape,
            border = border,
            colors = ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = content,
                disabledContainerColor = container.copy(alpha = 0.5f),
                disabledContentColor = content.copy(alpha = 0.8f),
            ),
            contentPadding = padding,
        ) {
            ButtonContent(text = text, isLoading = isLoading, leadingIcon = leadingIcon, contentColor = content)
        }
    }
}

@Composable
private fun AppButtonVariant.containerColor(): Color {
    val colors = AppTheme.colors
    return when (this) {
        AppButtonVariant.Primary -> colors.primary
        AppButtonVariant.Confirm -> colors.confirm
        AppButtonVariant.Danger -> colors.danger
        AppButtonVariant.Secondary -> colors.secondary
    }
}

@Composable
private fun AppButtonVariant.contentColor(): Color {
    val colors = AppTheme.colors
    return when (this) {
        AppButtonVariant.Primary -> colors.onPrimary
        AppButtonVariant.Confirm -> colors.onConfirm
        AppButtonVariant.Danger -> colors.onDanger
        AppButtonVariant.Secondary -> colors.onSecondary
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: ImageVector?,
    contentColor: Color,
) {
    val dimens = AppTheme.dimens
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = contentColor,
            strokeWidth = 2.dp,
        )
    } else {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(dimens.iconSize))
            Spacer(Modifier.width(dimens.spaceS))
        }
        Text(text = text, style = AppTheme.typography.button)
    }
}

/** Botón de solo texto subrayado (ej. "Atrás" en el formulario por pasos). */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(
            text = text,
            style = AppTheme.typography.button.copy(textDecoration = TextDecoration.Underline),
            color = if (enabled) AppTheme.colors.text else AppTheme.colors.text.copy(alpha = 0.4f),
        )
    }
}

// ---- Atajos por variante (más legibles en las pantallas) ----

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) = AppButton(text, onClick, modifier, AppButtonVariant.Primary, enabled, isLoading, leadingIcon)

@Composable
fun ConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) = AppButton(text, onClick, modifier, AppButtonVariant.Confirm, enabled, isLoading, leadingIcon)

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) = AppButton(text, onClick, modifier, AppButtonVariant.Danger, enabled, isLoading, leadingIcon)

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) = AppButton(text, onClick, modifier, AppButtonVariant.Secondary, enabled, isLoading, leadingIcon)
