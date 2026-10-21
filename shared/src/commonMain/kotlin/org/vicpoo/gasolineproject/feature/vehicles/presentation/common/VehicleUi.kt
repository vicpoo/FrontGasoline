package org.vicpoo.gasolineproject.feature.vehicles.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.vehicle_status_available
import gasolineproject.shared.generated.resources.vehicle_status_in_use
import gasolineproject.shared.generated.resources.vehicle_status_maintenance
import gasolineproject.shared.generated.resources.vehicle_status_unknown
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.components.StatusChip
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleStatus

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
