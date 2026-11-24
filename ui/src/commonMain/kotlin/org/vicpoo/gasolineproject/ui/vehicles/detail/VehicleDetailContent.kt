package org.vicpoo.gasolineproject.ui.vehicles.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.domain.common.formatDecimal
import org.vicpoo.gasolineproject.domain.vehicles.model.Vehicle
import org.vicpoo.gasolineproject.domain.vehicles.model.toImageModel
import org.vicpoo.gasolineproject.ui.common.components.AppBackButton
import org.vicpoo.gasolineproject.ui.common.components.AppButton
import org.vicpoo.gasolineproject.ui.common.components.AppButtonVariant
import org.vicpoo.gasolineproject.ui.common.components.AppImage
import org.vicpoo.gasolineproject.ui.common.components.ErrorText
import org.vicpoo.gasolineproject.ui.common.components.InfoRow
import org.vicpoo.gasolineproject.ui.common.components.PendingSyncChip
import org.vicpoo.gasolineproject.ui.common.components.PrimaryButton
import org.vicpoo.gasolineproject.ui.common.components.SecondaryText
import org.vicpoo.gasolineproject.ui.common.components.SyncErrorChip
import org.vicpoo.gasolineproject.ui.common.components.TitleText
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.action_delete
import org.vicpoo.gasolineproject.ui.resources.action_edit
import org.vicpoo.gasolineproject.ui.resources.reservation_request_button
import org.vicpoo.gasolineproject.ui.resources.vehicle_field_color
import org.vicpoo.gasolineproject.ui.resources.vehicle_field_mileage
import org.vicpoo.gasolineproject.ui.resources.vehicle_field_plates
import org.vicpoo.gasolineproject.ui.resources.vehicle_field_status
import org.vicpoo.gasolineproject.ui.resources.vehicle_field_tank
import org.vicpoo.gasolineproject.ui.resources.vehicle_field_type
import org.vicpoo.gasolineproject.ui.resources.vehicle_field_year
import org.vicpoo.gasolineproject.ui.resources.vehicle_km_value
import org.vicpoo.gasolineproject.ui.resources.vehicle_liters_value
import org.vicpoo.gasolineproject.ui.vehicles.common.icon
import org.vicpoo.gasolineproject.ui.vehicles.common.label

/* Contenido del detalle de un vehículo en celular y en pantalla ancha (separado de VehicleDetailScreen, punto 3.2.3). */

/** Teléfono: foto arriba a todo lo ancho, datos abajo y los botones fijos abajo de la pantalla. */
@Composable
internal fun DetailCompact(
    vehicle: Vehicle,
    state: VehicleDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEvent: (VehicleDetailEvent) -> Unit,
) {
    val dimens = AppTheme.dimens
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .navigationBarsPadding(),
    ) {
        // Los botones de abajo quedan un 5% de la pantalla arriba del borde
        val bottomGap = maxHeight * 0.05f
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth()) {
                    AppImage(
                        model = vehicle.photo?.toImageModel(),
                        fallbackIcon = vehicle.type.icon,
                        contentDescription = vehicle.model,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
                    )
                    AppBackButton(
                        onClick = onBack,
                        modifier = Modifier.statusBarsPadding().padding(dimens.spaceM),
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = dimens.maxContentWidth)
                        .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceL),
                ) {
                    VehicleInfo(vehicle, state, onEdit, onEvent, showActions = false)
                }
            }

            // Botones fijos abajo (Enviar solicitud / Editar · Eliminar), todos a la misma altura:
            // siempre a la mano sin tener que hacer scroll.
            if (state.canRequest || state.canEdit || state.canDelete) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = dimens.screenPadding, end = dimens.screenPadding, top = dimens.spaceM, bottom = bottomGap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                ) {
                    if (state.canRequest) {
                        PrimaryButton(
                            text = stringResource(Res.string.reservation_request_button),
                            onClick = { onEvent(VehicleDetailEvent.RequestClicked) },
                            modifier = Modifier.fillMaxWidth().widthIn(max = dimens.maxContentWidth),
                        )
                    }
                    if (state.canEdit || state.canDelete) {
                        EditDeleteButtons(
                            state = state,
                            onEdit = onEdit,
                            onEvent = onEvent,
                            modifier = Modifier.fillMaxWidth().widthIn(max = dimens.maxContentWidth),
                        )
                    }
                }
            }
        }
    }
}

/** Tablet horizontal: foto a la izquierda, datos a la derecha. */
@Composable
internal fun DetailExpanded(
    vehicle: Vehicle,
    state: VehicleDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEvent: (VehicleDetailEvent) -> Unit,
) {
    val dimens = AppTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(dimens.spaceL),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl),
    ) {
        Box(Modifier.weight(1f)) {
            AppImage(
                model = vehicle.photo?.toImageModel(),
                fallbackIcon = vehicle.type.icon,
                contentDescription = vehicle.model,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(dimens.radius)),
            )
            AppBackButton(onClick = onBack, modifier = Modifier.padding(dimens.spaceM))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            VehicleInfo(vehicle, state, onEdit, onEvent)
        }
    }
}

@Composable
internal fun ColumnScope.VehicleInfo(
    vehicle: Vehicle,
    state: VehicleDetailUiState,
    onEdit: () -> Unit,
    onEvent: (VehicleDetailEvent) -> Unit,
    /** false = los botones van fijos abajo de la pantalla (teléfono), no después de los datos. */
    showActions: Boolean = true,
) {
    val dimens = AppTheme.dimens
    TitleText(vehicle.model)
    SecondaryText(vehicle.brandAndModel)
    if (vehicle.isPendingSync || vehicle.hasSyncError) {
        Spacer(Modifier.height(dimens.spaceS))
        if (vehicle.hasSyncError) SyncErrorChip() else PendingSyncChip()
    }
    vehicle.syncErrorMessage?.takeIf { it.isNotBlank() }?.let {
        Spacer(Modifier.height(dimens.spaceXs))
        ErrorText(it)
    }
    Spacer(Modifier.height(dimens.spaceL))

    InfoRow(stringResource(Res.string.vehicle_field_type), vehicle.type.label())
    InfoRow(stringResource(Res.string.vehicle_field_plates), vehicle.plates)
    InfoRow(stringResource(Res.string.vehicle_field_year), vehicle.year?.toString() ?: "—")
    InfoRow(stringResource(Res.string.vehicle_field_color), vehicle.color ?: "—")
    InfoRow(stringResource(Res.string.vehicle_field_mileage), stringResource(Res.string.vehicle_km_value, formatDecimal(vehicle.mileageKm)))
    InfoRow(
        stringResource(Res.string.vehicle_field_tank),
        vehicle.tankCapacityLiters?.let { stringResource(Res.string.vehicle_liters_value, formatDecimal(it)) } ?: "—",
    )
    InfoRow(stringResource(Res.string.vehicle_field_status), vehicle.status.label())

    state.error?.let {
        Spacer(Modifier.height(dimens.spaceM))
        ErrorText(it.asText())
    }

    // RBAC: cada botón aparece solo si el usuario tiene el privilegio
    if (!showActions) return

    if (state.canRequest) {
        Spacer(Modifier.height(dimens.spaceXl))
        AppButton(
            text = stringResource(Res.string.reservation_request_button),
            onClick = { onEvent(VehicleDetailEvent.RequestClicked) },
            modifier = Modifier.fillMaxWidth(),
            variant = AppButtonVariant.Secondary,
            outlined = true,
        )
    }
    if (state.canEdit || state.canDelete) {
        Spacer(Modifier.height(dimens.spaceXl))
        EditDeleteButtons(state, onEdit, onEvent, Modifier.fillMaxWidth())
    }
}

/** Editar / Eliminar lado a lado (cada uno solo si el usuario tiene el privilegio). */
@Composable
internal fun EditDeleteButtons(
    state: VehicleDetailUiState,
    onEdit: () -> Unit,
    onEvent: (VehicleDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = AppTheme.dimens
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(dimens.spaceM)) {
        if (state.canEdit) {
            AppButton(
                text = stringResource(Res.string.action_edit),
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                variant = AppButtonVariant.Secondary,
                outlined = true,
            )
        }
        if (state.canDelete) {
            AppButton(
                text = stringResource(Res.string.action_delete),
                onClick = { onEvent(VehicleDetailEvent.DeleteClicked) },
                modifier = Modifier.weight(1f),
                variant = AppButtonVariant.Danger,
                outlined = true,
            )
        }
    }
}
