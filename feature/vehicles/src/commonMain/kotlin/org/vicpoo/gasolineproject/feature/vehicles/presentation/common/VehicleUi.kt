package org.vicpoo.gasolineproject.feature.vehicles.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.components.StatusChip
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleStatus
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleType
import org.vicpoo.gasolineproject.feature.vehicles.resources.Res
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_status_available
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_status_in_use
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_status_maintenance
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_status_unknown
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_type_car
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_type_motorcycle
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_type_pickup
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_type_truck

@Composable
fun VehicleStatus.label(): String = when (this) {
    VehicleStatus.AVAILABLE -> stringResource(Res.string.vehicle_status_available)
    VehicleStatus.IN_USE -> stringResource(Res.string.vehicle_status_in_use)
    VehicleStatus.MAINTENANCE -> stringResource(Res.string.vehicle_status_maintenance)
    VehicleStatus.UNKNOWN -> stringResource(Res.string.vehicle_status_unknown)
}

/** Píldora de estado con el color de la guía según el estado. */
@Composable
fun VehicleStatusChip(status: VehicleStatus, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val (container, content) = when (status) {
        VehicleStatus.AVAILABLE -> colors.primary to colors.onPrimary
        VehicleStatus.IN_USE -> colors.secondary to colors.onSecondary
        VehicleStatus.MAINTENANCE -> colors.danger to colors.onDanger
        VehicleStatus.UNKNOWN -> colors.labelBackground to colors.onLabel
    }
    StatusChip(text = status.label(), containerColor = container, contentColor = content, modifier = modifier)
}

/** Ícono de la tarjeta según el tipo (todos con el mismo trazo de la guía). */
val VehicleType.icon: ImageVector
    get() = when (this) {
        VehicleType.CAR -> AppIcons.Car
        VehicleType.MOTORCYCLE -> AppIcons.Motorbike
        VehicleType.PICKUP -> AppIcons.Pickup
        VehicleType.TRUCK -> AppIcons.Truck
    }

@Composable
fun VehicleType.label(): String = when (this) {
    VehicleType.CAR -> stringResource(Res.string.vehicle_type_car)
    VehicleType.MOTORCYCLE -> stringResource(Res.string.vehicle_type_motorcycle)
    VehicleType.PICKUP -> stringResource(Res.string.vehicle_type_pickup)
    VehicleType.TRUCK -> stringResource(Res.string.vehicle_type_truck)
}
