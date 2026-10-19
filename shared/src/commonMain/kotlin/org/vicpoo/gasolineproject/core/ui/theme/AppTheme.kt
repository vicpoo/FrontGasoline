package org.vicpoo.gasolineproject.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }
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
    val colors = if (darkTheme) DarkAppColors else LightAppColors
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
