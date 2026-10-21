package org.vicpoo.gasolineproject.feature.vehicles.presentation.detail

import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehiclesMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.action_delete
import gasolineproject.shared.generated.resources.action_edit
import gasolineproject.shared.generated.resources.action_no
import gasolineproject.shared.generated.resources.action_yes
import gasolineproject.shared.generated.resources.vehicle_delete_confirm
import gasolineproject.shared.generated.resources.vehicle_field_color
import gasolineproject.shared.generated.resources.vehicle_field_mileage
import gasolineproject.shared.generated.resources.vehicle_field_plates
import gasolineproject.shared.generated.resources.vehicle_field_status
import gasolineproject.shared.generated.resources.vehicle_field_tank
import gasolineproject.shared.generated.resources.vehicle_field_year
import gasolineproject.shared.generated.resources.vehicle_km_value
import gasolineproject.shared.generated.resources.vehicle_liters_value
import gasolineproject.shared.generated.resources.vehicle_not_found
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.ResultDialog
import org.vicpoo.gasolineproject.core.ui.components.ResultType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import gasolineproject.shared.generated.resources.action_retry
import gasolineproject.shared.generated.resources.action_understood
import gasolineproject.shared.generated.resources.reservation_error_message
import gasolineproject.shared.generated.resources.reservation_error_title
import gasolineproject.shared.generated.resources.reservation_reason_hint
import gasolineproject.shared.generated.resources.reservation_reason_label
import gasolineproject.shared.generated.resources.reservation_request_button
import gasolineproject.shared.generated.resources.reservation_send
import gasolineproject.shared.generated.resources.reservation_success_message
import gasolineproject.shared.generated.resources.reservation_success_title
import org.vicpoo.gasolineproject.core.ui.adaptive.LocalWindowWidth
import org.vicpoo.gasolineproject.core.ui.adaptive.WindowWidth
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.AppButton
import org.vicpoo.gasolineproject.core.ui.components.AppButtonVariant
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.components.ConfirmDialog
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.InfoRow
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.toImageModel
import org.vicpoo.gasolineproject.core.ui.components.PendingSyncChip
import org.vicpoo.gasolineproject.core.ui.components.SyncErrorChip
import org.vicpoo.gasolineproject.core.common.formatDecimal
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.label

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
        )
    }

    // ---- Solicitud de reserva ----
    val vehicleName = vehicle?.model.orEmpty()
    if (state.showRequestDialog) {
        RequestReasonDialog(state = state, onEvent = onEvent)
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
}

/** Figma: "Motivo de la solicitud" + área de texto + "Enviar". */
@Composable
private fun RequestReasonDialog(state: VehicleDetailUiState, onEvent: (VehicleDetailEvent) -> Unit) {
    val dimens = AppTheme.dimens
    Dialog(onDismissRequest = { if (!state.isSendingRequest) onEvent(VehicleDetailEvent.RequestDismissed) }) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp),
            shape = RoundedCornerShape(dimens.radiusM * 2),
            color = AppTheme.colors.background,
        ) {
            Column(Modifier.padding(dimens.spaceL), horizontalAlignment = Alignment.CenterHorizontally) {
                AppTextField(
                    value = state.requestReason,
                    onValueChange = { onEvent(VehicleDetailEvent.RequestReasonChanged(it)) },
                    label = stringResource(Res.string.reservation_reason_label),
                    placeholder = stringResource(Res.string.reservation_reason_hint),
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = state.requestReasonError?.asText(),
                    enabled = !state.isSendingRequest,
                    singleLine = false,
                    minLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(dimens.spaceL))
                PrimaryButton(
                    text = stringResource(Res.string.reservation_send),
                    onClick = { onEvent(VehicleDetailEvent.SendRequest) },
                    isLoading = state.isSendingRequest,
                    modifier = Modifier.widthIn(min = 160.dp),
                )
            }
        }
    }
}

/** Teléfono: foto arriba a todo lo ancho, datos abajo. */
@Composable
private fun DetailCompact(
    vehicle: Vehicle,
    state: VehicleDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEvent: (VehicleDetailEvent) -> Unit,
) {
    val dimens = AppTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth()) {
            AppImage(
                model = vehicle.photo?.toImageModel(),
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
            VehicleInfo(vehicle, state, onEdit, onEvent)
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
                contentDescription = vehicle.model,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(dimens.radiusM)),
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceM)) {
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
}
