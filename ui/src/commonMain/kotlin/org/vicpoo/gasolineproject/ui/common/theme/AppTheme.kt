package org.vicpoo.gasolineproject.ui.common.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Duración del cambio claro <-> oscuro (los colores se mezclan en vez de cambiar de golpe). */
private const val THEME_ANIMATION_MILLIS = 450

// compositionLocalOf (no static): durante la transición solo se repinta lo que lee cada color
private val LocalAppColors = compositionLocalOf { LightAppColors }
private val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("AppTypography no está disponible: envuelve tu UI con AppTheme { }")
}
private val LocalAppDimens = staticCompositionLocalOf { AppDimens() }

/**
 * Tema de la app. Expone colores, tipografía y dimensiones propios
 * y además configura MaterialTheme para que los componentes de Material3
 * también usen la misma paleta.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Al cambiar de modo los colores pasan suavemente del claro al oscuro (y al revés)
    val colors = animateAppColors(if (darkTheme) DarkAppColors else LightAppColors)
    val typography = appTypography()
    val dimens = remember { AppDimens() }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppDimens provides dimens,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = typography.toMaterialTypography(),
            content = content,
        )
    }
}

/** Acceso a los tokens del tema: AppTheme.colors.primary, AppTheme.typography.title, ... */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current

    val typography: AppTypography
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current

    val dimens: AppDimens
        @Composable @ReadOnlyComposable get() = LocalAppDimens.current
}

/** Cada color del tema se anima hacia el del modo nuevo. */
@Composable
private fun animateAppColors(target: AppColors): AppColors = target.copy(
    background = target.background.animated("background"),
    surface = target.surface.animated("surface"),
    card = target.card.animated("card"),
    title = target.title.animated("title"),
    subtitle = target.subtitle.animated("subtitle"),
    text = target.text.animated("text"),
    textSecondary = target.textSecondary.animated("textSecondary"),
    primary = target.primary.animated("primary"),
    onPrimary = target.onPrimary.animated("onPrimary"),
    confirm = target.confirm.animated("confirm"),
    onConfirm = target.onConfirm.animated("onConfirm"),
    danger = target.danger.animated("danger"),
    onDanger = target.onDanger.animated("onDanger"),
    secondary = target.secondary.animated("secondary"),
    onSecondary = target.onSecondary.animated("onSecondary"),
    secondaryBorder = target.secondaryBorder.animated("secondaryBorder"),
    labelBackground = target.labelBackground.animated("labelBackground"),
    onLabel = target.onLabel.animated("onLabel"),
    inputBackground = target.inputBackground.animated("inputBackground"),
    onInput = target.onInput.animated("onInput"),
    inputPlaceholder = target.inputPlaceholder.animated("inputPlaceholder"),
    error = target.error.animated("error"),
    divider = target.divider.animated("divider"),
    illustration = target.illustration.animated("illustration"),
)

@Composable
private fun Color.animated(label: String): Color = animateColorAsState(
    targetValue = this,
    animationSpec = tween(durationMillis = THEME_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
    label = label,
).value

private fun AppColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        tertiary = confirm,
        onTertiary = onConfirm,
        error = error,
        onError = onDanger,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = labelBackground,
        onSurfaceVariant = onLabel,
        outline = divider,
    )
}

private fun AppTypography.toMaterialTypography(): Typography {
    val base = Typography()
    return base.copy(
        headlineLarge = title,
        headlineMedium = subtitle,
        headlineSmall = subtitle,
        titleLarge = subtitle,
        bodyLarge = body,
        bodyMedium = bodySecondary,
        bodySmall = caption,
        labelLarge = button,
    )
}
