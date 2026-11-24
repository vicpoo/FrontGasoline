package org.vicpoo.gasolineproject.ui.vehicles.detail

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.ui.common.adaptive.LocalWindowWidth
import org.vicpoo.gasolineproject.ui.common.adaptive.WindowWidth
import org.vicpoo.gasolineproject.ui.common.components.AppBackButton
import org.vicpoo.gasolineproject.ui.common.components.ConfirmDialog
import org.vicpoo.gasolineproject.ui.common.components.EmptyView
import org.vicpoo.gasolineproject.ui.common.components.LoadingView
import org.vicpoo.gasolineproject.ui.common.components.ResultDialog
import org.vicpoo.gasolineproject.ui.common.components.ResultType
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.vehicles.common.icon
import org.vicpoo.gasolineproject.ui.vehicles.list.VehiclesMode
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.action_no
import org.vicpoo.gasolineproject.ui.resources.action_retry
import org.vicpoo.gasolineproject.ui.resources.action_understood
import org.vicpoo.gasolineproject.ui.resources.action_yes
import org.vicpoo.gasolineproject.ui.resources.reservation_error_message
import org.vicpoo.gasolineproject.ui.resources.reservation_error_title
import org.vicpoo.gasolineproject.ui.resources.reservation_queued_message
import org.vicpoo.gasolineproject.ui.resources.reservation_queued_title
import org.vicpoo.gasolineproject.ui.resources.reservation_success_message
import org.vicpoo.gasolineproject.ui.resources.reservation_success_title
import org.vicpoo.gasolineproject.ui.resources.vehicle_delete_confirm
import org.vicpoo.gasolineproject.ui.resources.vehicle_not_found

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
