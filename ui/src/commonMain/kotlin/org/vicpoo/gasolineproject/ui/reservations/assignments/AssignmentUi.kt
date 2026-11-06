package org.vicpoo.gasolineproject.ui.reservations.assignments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.ui.common.components.StatusChip
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.domain.reservations.AssignmentVehicleType
import org.vicpoo.gasolineproject.domain.reservations.VehicleState
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.assignments_active_trip
import org.vicpoo.gasolineproject.ui.resources.assignments_state_available
import org.vicpoo.gasolineproject.ui.resources.assignments_state_maintenance
import org.vicpoo.gasolineproject.ui.resources.assignments_state_occupied

/*
 * Piezas que comparten la lista y el detalle de Asignaciones.
 * Sin fotos: igual que en Vehículos, cada tarjeta lleva el ícono del tipo de vehículo
 * (o el de persona para los empleados) en un círculo azul claro.
 */

/** Ícono según el tipo de vehículo (los mismos de la vista Vehículos). */
internal val AssignmentVehicleType.icon: ImageVector
    get() = when (this) {
        AssignmentVehicleType.CAR -> AppIcons.Car
        AssignmentVehicleType.MOTORCYCLE -> AppIcons.Motorbike
        AssignmentVehicleType.PICKUP -> AppIcons.Pickup
        AssignmentVehicleType.TRUCK -> AppIcons.Truck
    }

/** Ícono en un círculo azul claro (como VehicleCard de Vehículos). */
@Composable
internal fun IconBadge(icon: ImageVector, contentDescription: String?, size: Dp = 48.dp) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = colors.primary, modifier = Modifier.size(size * 0.54f))
    }
}

@Composable
internal fun VehicleStateChip(state: VehicleState) {
    val colors = AppTheme.colors
    when (state) {
        VehicleState.AVAILABLE -> StatusChip(stringResource(Res.string.assignments_state_available), colors.confirm, colors.onConfirm)
        VehicleState.OCCUPIED -> StatusChip(stringResource(Res.string.assignments_state_occupied), colors.primary, colors.onPrimary)
        VehicleState.MAINTENANCE -> StatusChip(stringResource(Res.string.assignments_state_maintenance), colors.danger, colors.onDanger)
        VehicleState.UNKNOWN -> Unit
    }
}

@Composable
internal fun ActiveTripChip() {
    val colors = AppTheme.colors
    StatusChip(stringResource(Res.string.assignments_active_trip), colors.secondary, colors.onSecondary)
}
