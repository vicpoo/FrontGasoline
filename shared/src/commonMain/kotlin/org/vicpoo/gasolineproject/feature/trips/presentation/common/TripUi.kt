package org.vicpoo.gasolineproject.feature.trips.presentation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.components.UserAvatar
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Foto circular + nombre del operador (encabezado de tarjeta y detalle del viaje). */
@Composable
fun OperatorHeader(
    name: String,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 36.dp,
    textStyle: TextStyle = AppTheme.typography.body,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(photoUrl = photoUrl, size = avatarSize)
        Spacer(Modifier.width(AppTheme.dimens.spaceS))
        Text(name, style = textStyle, color = AppTheme.colors.title)
    }
}
