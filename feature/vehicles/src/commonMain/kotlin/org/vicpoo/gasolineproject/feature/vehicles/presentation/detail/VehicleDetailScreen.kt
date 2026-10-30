package org.vicpoo.gasolineproject.feature.vehicles.presentation.detail

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.formatDecimal
import org.vicpoo.gasolineproject.core.ui.adaptive.LocalWindowWidth
import org.vicpoo.gasolineproject.core.ui.adaptive.WindowWidth
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.AppButton
import org.vicpoo.gasolineproject.core.ui.components.AppButtonVariant
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
import org.vicpoo.gasolineproject.core.ui.components.ConfirmDialog
import org.vicpoo.gasolineproject.core.ui.components.EditSheet
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.InfoRow
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.PendingSyncChip
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.ResultDialog
import org.vicpoo.gasolineproject.core.ui.components.ResultType
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.SyncErrorChip
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.api.VehicleRequester
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.toImageModel
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.icon
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.label
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehiclesMode
import org.vicpoo.gasolineproject.feature.vehicles.resources.Res
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_cancel
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_delete
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_edit
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_no
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_retry
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_understood
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_yes
import org.vicpoo.gasolineproject.feature.vehicles.resources.cd_close
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_error_message
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_error_title
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_queued_message
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_queued_title
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_reason_hint
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_reason_label
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_request_button
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_send
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_sheet_counter
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_sheet_send
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_sheet_subtitle
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_sheet_title
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_success_message
import org.vicpoo.gasolineproject.feature.vehicles.resources.reservation_success_title
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_delete_confirm
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_color
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_mileage
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_plates
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_status
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_tank
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_type
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_year
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_km_value
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_liters_value
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_not_found

@Composable
fun VehicleDetailRoute(
    vehicleId: String,
    mode: VehiclesMode,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: VehicleDetailViewModel = koinViewModel(key = "vehicle-detail-$mode-$vehicleId") {
        parametersOf(VehicleDetailArgs(vehicleId, mode))
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                VehicleDetailEffect.Deleted -> onBack()
            }
        }
    }

    VehicleDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = { onEdit(vehicleId) },
        onEvent = viewModel::onEvent,
    )
}

/** Figma: detalle con foto grande, datos y botones Editar / Eliminar (según privilegios). */
@Composable
fun VehicleDetailScreen(
    state: VehicleDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEvent: (VehicleDetailEvent) -> Unit,
) {
    val vehicle = state.vehicle
    when {
        state.isLoading -> LoadingView()
        vehicle == null -> Box(Modifier.fillMaxSize()) {
            EmptyView(stringResource(Res.string.vehicle_not_found))
            AppBackButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(AppTheme.dimens.spaceM))
        }
        LocalWindowWidth.current == WindowWidth.Expanded -> DetailExpanded(vehicle, state, onBack, onEdit, onEvent)
        else -> DetailCompact(vehicle, state, onBack, onEdit, onEvent)
    }

    if (state.showDeleteConfirm) {
        ConfirmDialog(
            message = stringResource(Res.string.vehicle_delete_confirm),
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = { onEvent(VehicleDetailEvent.DeleteConfirmed) },
            onDismiss = { onEvent(VehicleDetailEvent.DeleteDismissed) },
            isLoading = state.isDeleting,
            icon = AppIcons.Trash,
        )
    }

    // ---- Solicitud de reserva ----
    val vehicleName = vehicle?.model.orEmpty()
    if (state.showRequestDialog) {
        RequestReasonSheet(vehicle = vehicle, state = state, onEvent = onEvent)
    }
    state.requestError?.let { error ->
        ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.reservation_error_title),
            message = if (error == AppError.NoConnection) {
                stringResource(Res.string.reservation_error_message, vehicleName)
            } else {
                error.asText()
            },
            buttonText = stringResource(Res.string.action_retry),
            onButtonClick = {
                onEvent(VehicleDetailEvent.RequestResultDismissed)
                onEvent(VehicleDetailEvent.SendRequest)
            },
            onDismiss = { onEvent(VehicleDetailEvent.RequestResultDismissed) },
        )
    }
    if (state.requestSent) {
        ResultDialog(
            type = ResultType.Success,
            title = stringResource(Res.string.reservation_success_title),
            message = stringResource(Res.string.reservation_success_message, vehicleName),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = { onEvent(VehicleDetailEvent.RequestResultDismissed) },
            onDismiss = { onEvent(VehicleDetailEvent.RequestResultDismissed) },
        )
    }
    // Sin conexión: quedó en cola y se enviará sola
    if (state.requestQueued) {
        ResultDialog(
            type = ResultType.Success,
            title = stringResource(Res.string.reservation_queued_title),
            message = stringResource(Res.string.reservation_queued_message, vehicleName),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = { onEvent(VehicleDetailEvent.RequestResultDismissed) },
            onDismiss = { onEvent(VehicleDetailEvent.RequestResultDismissed) },
        )
    }
}

/**
 * Enviar solicitud estilo Airbnb: hoja que sube desde abajo con título grande, el carro que se
 * pide (foto, nombre y placas), el motivo con contador y abajo "Cancelar" / "Enviar solicitud".
 */
@Composable
private fun RequestReasonSheet(vehicle: Vehicle?, state: VehicleDetailUiState, onEvent: (VehicleDetailEvent) -> Unit) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    EditSheet(
        title = stringResource(Res.string.reservation_sheet_title),
        subtitle = stringResource(Res.string.reservation_sheet_subtitle),
        cancelText = stringResource(Res.string.action_cancel),
        saveText = stringResource(Res.string.reservation_sheet_send),
        onCancel = { onEvent(VehicleDetailEvent.RequestDismissed) },
        onSave = { onEvent(VehicleDetailEvent.SendRequest) },
        isSaving = state.isSendingRequest,
        closeDescription = stringResource(Res.string.cd_close),
    ) {
        // El carro que se está pidiendo
        if (vehicle != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimens.radius),
                color = colors.background,
                border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.25f)),
            ) {
                Row(Modifier.padding(dimens.spaceM), verticalAlignment = Alignment.CenterVertically) {
                    AppImage(
                        model = vehicle.photo?.toImageModel(),
                        fallbackIcon = vehicle.type.icon,
                        contentDescription = vehicle.cardTitle,
                        modifier = Modifier.size(width = 88.dp, height = 72.dp).clip(RoundedCornerShape(dimens.radius)),
                    )
                    Spacer(Modifier.width(dimens.spaceM))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = vehicle.cardTitle,
                            style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.title,
                        )
                        Text(vehicle.plates, style = AppTheme.typography.bodySecondary, color = colors.textSecondary)
                    }
                }
            }
            Spacer(Modifier.height(dimens.spaceL))
        }

        AppTextField(
            value = state.requestReason,
            onValueChange = { onEvent(VehicleDetailEvent.RequestReasonChanged(it)) },
            label = stringResource(Res.string.reservation_reason_label),
            placeholder = stringResource(Res.string.reservation_reason_hint),
            modifier = Modifier.fillMaxWidth(),
            errorMessage = state.requestReasonError?.asText(),
            enabled = !state.isSendingRequest,
            singleLine = false,
            minLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        // Contador como en Airbnb (máx. 255, igual que la API)
        Text(
            text = stringResource(Res.string.reservation_sheet_counter, state.requestReason.length, VehicleRequester.MAX_REASON),
            style = AppTheme.typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

/** Teléfono: foto arriba a todo lo ancho, datos abajo y los botones fijos abajo de la pantalla. */
@Composable
private fun DetailCompact(
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
private fun DetailExpanded(
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
private fun ColumnScope.VehicleInfo(
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
private fun EditDeleteButtons(
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
