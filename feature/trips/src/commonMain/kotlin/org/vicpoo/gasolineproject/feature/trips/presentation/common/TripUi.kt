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
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.common.AppDates
import org.vicpoo.gasolineproject.core.ui.components.MapMarker
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.feature.trips.domain.Trip
import org.vicpoo.gasolineproject.feature.trips.resources.Res
import org.vicpoo.gasolineproject.feature.trips.resources.trip_fuel_marker

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

/**
 * Cargas del viaje que tienen ubicación, como marcadores para el mapa (icono de gasolina).
 * Las que no tienen ubicación simplemente no se dibujan.
 */
@Composable
fun Trip.fuelMarkers(): List<MapMarker> {
    val description = stringResource(Res.string.trip_fuel_marker)
    return remember(fuelLoads, description) {
        fuelLoads
            .sortedBy { AppDates.sortKey(it.date) }
            .mapNotNull { load -> load.location?.let { MapMarker(point = it, icon = AppIcons.Fuel, description = description) } }
    }
}
