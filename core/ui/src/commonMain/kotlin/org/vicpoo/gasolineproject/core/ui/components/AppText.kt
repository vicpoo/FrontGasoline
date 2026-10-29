package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Título de vista (24 sp, AppTextSize.Title). */
@Composable
fun TitleText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Unspecified) {
    Text(text = text, modifier = modifier, style = AppTheme.typography.title, color = AppTheme.colors.title, textAlign = textAlign)
}

/** Subtítulo (18 sp, AppTextSize.Subtitle). */
@Composable
fun SubtitleText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Unspecified) {
    Text(text = text, modifier = modifier, style = AppTheme.typography.subtitle, color = AppTheme.colors.subtitle, textAlign = textAlign)
}

/** Texto general (15 sp, AppTextSize.Body). */
@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Unspecified) {
    Text(text = text, modifier = modifier, style = AppTheme.typography.body, color = AppTheme.colors.text, textAlign = textAlign)
}

/** Texto secundario (14 sp, AppTextSize.BodySecondary). */
@Composable
fun SecondaryText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Unspecified) {
    Text(text = text, modifier = modifier, style = AppTheme.typography.bodySecondary, color = AppTheme.colors.textSecondary, textAlign = textAlign)
}

/** Mensaje de error en línea (debajo de un campo o formulario). */
@Composable
fun ErrorText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Unspecified) {
    Text(text = text, modifier = modifier, style = AppTheme.typography.caption, color = AppTheme.colors.error, textAlign = textAlign)
}
