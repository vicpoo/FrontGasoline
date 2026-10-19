package org.vicpoo.gasolineproject.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.inter_bold
import gasolineproject.shared.generated.resources.inter_medium
import gasolineproject.shared.generated.resources.inter_regular
import gasolineproject.shared.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

/**
 * Tamaños de la guía de enmaquetado (px de Figma = sp en Compose).
 * Cambia aquí y se actualiza toda la app.
 */
object AppTextSize {
    val Title = 36.sp
    val Subtitle = 24.sp
    val Body = 18.sp
    val BodySecondary = 18.sp
    val Button = 18.sp
    val Caption = 14.sp
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

@Composable
fun interFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

@Composable
fun appTypography(): AppTypography {
    val inter = interFontFamily()
    return remember(inter) {
        AppTypography(
            title = TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.Title,
                lineHeight = 44.sp,
            ),
            subtitle = TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.Subtitle,
                lineHeight = 30.sp,
            ),
            body = TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.Body,
                lineHeight = 24.sp,
            ),
            bodySecondary = TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.BodySecondary,
                lineHeight = 24.sp,
            ),
            button = TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.Button,
                lineHeight = 24.sp,
            ),
            caption = TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Normal,
                fontSize = AppTextSize.Caption,
                lineHeight = 18.sp,
            ),
        )
    }
}
