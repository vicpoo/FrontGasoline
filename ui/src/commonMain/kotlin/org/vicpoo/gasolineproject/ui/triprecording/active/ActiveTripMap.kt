package org.vicpoo.gasolineproject.ui.triprecording.active

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.ui.common.components.AppBackButton
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.map.LiveRouteMap
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.active_trip_gps_approximate
import org.vicpoo.gasolineproject.ui.resources.active_trip_gps_no_permission
import org.vicpoo.gasolineproject.ui.resources.active_trip_gps_on
import org.vicpoo.gasolineproject.ui.resources.active_trip_gps_searching
import org.vicpoo.gasolineproject.ui.resources.active_trip_paused

/* Mapa del viaje en curso y el aviso de GPS (separados de ActiveTripScreen, punto 3.2.3). */

/** Mapa con la ruta + arriba el botón de regresar y el aviso del GPS. */
@Composable
internal fun TripMap(
    state: ActiveTripUiState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val session = state.session
    Box(modifier.fillMaxHeight()) {
        // Icono de gasolina donde ya se registraron cargas en este viaje
        val fuelPoints = remember(state.fuelRecords) { state.fuelRecords.mapNotNull { it.location } }
        LiveRouteMap(
            route = session.route,
            current = session.current,
            fuelPoints = fuelPoints,
            modifier = Modifier.fillMaxSize(),
        )

        // Parte de arriba: regresar + aviso del GPS
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = colors.background, shadowElevation = 2.dp) {
                AppBackButton(onClick = onBack)
            }
            Spacer(Modifier.width(dimens.spaceS))
            GpsBanner(
                gpsActive = session.gpsActive,
                isPaused = state.isPaused,
                hasPermission = session.hasLocationPermission,
                hasPreciseLocation = session.hasPreciseLocation,
                onRequestPermission = onRequestPermission,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** "Señal de Gps activa" (azul) / en pausa / buscando o sin permiso (rojo). */
@Composable
internal fun GpsBanner(
    gpsActive: Boolean,
    isPaused: Boolean,
    hasPermission: Boolean,
    hasPreciseLocation: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    // Sin permiso o con ubicación "aproximada": tocar el aviso vuelve a pedir la ubicación precisa
    val canFix = !hasPermission || !hasPreciseLocation
    val (text, container, content) = when {
        isPaused -> Triple(stringResource(Res.string.active_trip_paused), colors.secondary, colors.onSecondary)
        !hasPermission -> Triple(stringResource(Res.string.active_trip_gps_no_permission), colors.danger, colors.onDanger)
        !hasPreciseLocation -> Triple(stringResource(Res.string.active_trip_gps_approximate), colors.danger, colors.onDanger)
        gpsActive -> Triple(stringResource(Res.string.active_trip_gps_on), colors.primary, colors.onPrimary)
        else -> Triple(stringResource(Res.string.active_trip_gps_searching), colors.secondary, colors.onSecondary)
    }
    Surface(
        modifier = modifier.then(if (canFix) Modifier.clickable(onClick = onRequestPermission) else Modifier),
        shape = RoundedCornerShape(dimens.radius),
        color = container,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimens.spaceM, vertical = dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Gps, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(dimens.spaceS))
            Text(text, style = AppTheme.typography.caption, color = content)
        }
    }
}
