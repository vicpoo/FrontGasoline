package org.vicpoo.gasolineproject.feature.vehicles.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.vehicle_pending_sync
import gasolineproject.shared.generated.resources.vehicle_sync_error
import gasolineproject.shared.generated.resources.vehicle_status_available
import gasolineproject.shared.generated.resources.vehicle_status_in_use
import gasolineproject.shared.generated.resources.vehicle_status_maintenance
import gasolineproject.shared.generated.resources.vehicle_status_unknown
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.components.StatusChip
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleStatus
import kotlin.math.roundToLong

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

/** Píldora gris "Pendiente de sincronizar" (offline-first). */
@Composable
fun PendingSyncChip(modifier: Modifier = Modifier) {
    StatusChip(
        text = stringResource(Res.string.vehicle_pending_sync),
        containerColor = AppTheme.colors.labelBackground,
        contentColor = AppTheme.colors.onLabel,
        modifier = modifier,
    )
}

/** Píldora roja "Error al sincronizar" (la API rechazó el cambio). */
@Composable
fun SyncErrorChip(modifier: Modifier = Modifier) {
    StatusChip(
        text = stringResource(Res.string.vehicle_sync_error),
        containerColor = AppTheme.colors.danger,
        contentColor = AppTheme.colors.onDanger,
        modifier = modifier,
    )
}

/** 50.0 -> "50", 45.5 -> "45.5", 18230.25 -> "18230.25". */
fun formatDecimal(value: Double): String {
    val rounded = (value * 100).roundToLong() / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
