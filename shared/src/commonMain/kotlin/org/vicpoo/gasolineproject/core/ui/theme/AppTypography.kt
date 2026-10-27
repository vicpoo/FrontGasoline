package org.vicpoo.gasolineproject.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.poppins_bold
import gasolineproject.shared.generated.resources.poppins_medium
import gasolineproject.shared.generated.resources.poppins_regular
import gasolineproject.shared.generated.resources.poppins_semibold
import org.jetbrains.compose.resources.Font

/**
 * Tamaños de texto de toda la app, basados en la escala de Material 3
 * (headlineSmall, titleMedium, bodyLarge, bodyMedium, labelLarge, bodySmall).
 * Cambia aquí y se actualiza toda la app.
 */
object AppTextSize {
    val Title = 24.sp
    val Subtitle = 18.sp
    val Body = 15.sp
    val BodySecondary = 14.sp
    val Button = 15.sp
    val Caption = 12.sp
}

@Immutable
data class AppTypography(
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val bodySecondary: TextStyle,
    val button: TextStyle,
    val caption: TextStyle,
)

/** Familia tipográfica principal: geométrica sans-serif (palo seco) — Poppins. */
@Composable
fun appFontFamily(): FontFamily = FontFamily(
    Font(Res.font.poppins_regular, FontWeight.Normal),
    Font(Res.font.poppins_medium, FontWeight.Medium),
    Font(Res.font.poppins_semibold, FontWeight.SemiBold),
    Font(Res.font.poppins_bold, FontWeight.Bold),
)

@Composable
fun appTypography(): AppTypography {
    val font = appFontFamily()
    return remember(font) {
        AppTypography(
            title = TextStyle(
                fontFamily = font,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.Title,
                lineHeight = 32.sp,
            ),
            subtitle = TextStyle(
                fontFamily = font,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.Subtitle,
                lineHeight = 24.sp,
            ),
            body = TextStyle(
                fontFamily = font,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.Body,
                lineHeight = 22.sp,
            ),
            bodySecondary = TextStyle(
                fontFamily = font,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.BodySecondary,
                lineHeight = 20.sp,
            ),
            button = TextStyle(
                fontFamily = font,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.Button,
                lineHeight = 20.sp,
            ),
            caption = TextStyle(
                fontFamily = font,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.Caption,
                lineHeight = 16.sp,
            ),
        )
    }
}
