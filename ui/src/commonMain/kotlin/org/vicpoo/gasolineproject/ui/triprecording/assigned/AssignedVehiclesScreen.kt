package org.vicpoo.gasolineproject.ui.triprecording.assigned

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.ui.common.components.AppButton
import org.vicpoo.gasolineproject.ui.common.components.AppButtonVariant
import org.vicpoo.gasolineproject.ui.common.components.AppCard
import org.vicpoo.gasolineproject.ui.common.components.AppTextField
import org.vicpoo.gasolineproject.ui.common.components.ResultDialog
import org.vicpoo.gasolineproject.ui.common.components.ResultType
import org.vicpoo.gasolineproject.ui.common.components.BodyText
import org.vicpoo.gasolineproject.ui.common.components.EmptyView
import org.vicpoo.gasolineproject.ui.common.components.ErrorText
import org.vicpoo.gasolineproject.ui.common.components.LoadingView
import org.vicpoo.gasolineproject.ui.common.components.OfflineBanner
import org.vicpoo.gasolineproject.ui.common.components.PrimaryButton
import org.vicpoo.gasolineproject.ui.common.components.ScreenHeader
import org.vicpoo.gasolineproject.ui.common.components.SecondaryText
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.components.EditSheet
import org.vicpoo.gasolineproject.ui.common.components.SheetCharCounter
import org.vicpoo.gasolineproject.ui.common.components.SheetSubjectCard
import org.vicpoo.gasolineproject.domain.triprecording.ReleaseVehicleUseCase
import org.vicpoo.gasolineproject.ui.resources.assigned_release_sheet_title
import org.vicpoo.gasolineproject.ui.resources.cd_close
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicle
import org.vicpoo.gasolineproject.domain.triprecording.RecordedTrip
import org.vicpoo.gasolineproject.domain.triprecording.ReleaseBlock
import org.vicpoo.gasolineproject.ui.triprecording.common.AssignedVehicleCard
import org.vicpoo.gasolineproject.ui.triprecording.common.TripFormat
import org.vicpoo.gasolineproject.ui.triprecording.syncstatus.SyncStatusUiState
import org.vicpoo.gasolineproject.ui.triprecording.syncstatus.SyncStatusViewModel
import org.vicpoo.gasolineproject.ui.resources.sync_status_banner
import org.vicpoo.gasolineproject.ui.resources.sync_status_banner_attention
import org.vicpoo.gasolineproject.ui.resources.sync_status_open
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.action_cancel
import org.vicpoo.gasolineproject.ui.resources.action_understood
import org.vicpoo.gasolineproject.ui.resources.assigned_release
import org.vicpoo.gasolineproject.ui.resources.assigned_release_blocked_active
import org.vicpoo.gasolineproject.ui.resources.assigned_release_blocked_offline
import org.vicpoo.gasolineproject.ui.resources.assigned_release_blocked_pending
import org.vicpoo.gasolineproject.ui.resources.assigned_release_done_message
import org.vicpoo.gasolineproject.ui.resources.assigned_release_done_title
import org.vicpoo.gasolineproject.ui.resources.assigned_release_error_title
import org.vicpoo.gasolineproject.ui.resources.assigned_release_info
import org.vicpoo.gasolineproject.ui.resources.assigned_release_notes
import org.vicpoo.gasolineproject.ui.resources.assigned_release_notes_hint
import org.vicpoo.gasolineproject.ui.resources.assigned_release_question
import org.vicpoo.gasolineproject.ui.resources.assigned_release_blocked_title
import org.vicpoo.gasolineproject.ui.resources.assigned_active_continue
import org.vicpoo.gasolineproject.ui.resources.assigned_active_title
import org.vicpoo.gasolineproject.ui.resources.assigned_empty
import org.vicpoo.gasolineproject.ui.resources.assigned_offline
import org.vicpoo.gasolineproject.ui.resources.assigned_start
import org.vicpoo.gasolineproject.ui.resources.assigned_title

@Composable
fun AssignedVehiclesRoute(
    onStartTrip: (reservationId: String) -> Unit,
    onOpenActiveTrip: () -> Unit,
    onOpenSyncStatus: () -> Unit,
    viewModel: AssignedVehiclesViewModel = koinViewModel(),
    syncViewModel: SyncStatusViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val syncState by syncViewModel.state.collectAsStateWithLifecycle()
    AssignedVehiclesScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onStartTrip = onStartTrip,
        onOpenActiveTrip = onOpenActiveTrip,
        syncState = syncState,
        onOpenSyncStatus = onOpenSyncStatus,
    )
}

/** Figma "Registrar": tarjetas de los carros asignados con el botón "▷ comienzo". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignedVehiclesScreen(
    state: AssignedVehiclesUiState,
    onEvent: (AssignedVehiclesEvent) -> Unit,
    onStartTrip: (String) -> Unit,
    onOpenActiveTrip: () -> Unit,
    syncState: SyncStatusUiState = SyncStatusUiState(),
    onOpenSyncStatus: () -> Unit = {},
) {
    val dimens = AppTheme.dimens
    Box(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = dimens.screenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = dimens.maxContentWidthWide)) {
            ScreenHeader(
                title = stringResource(Res.string.assigned_title),
                action = {
                    // "Envíos pendientes": siempre a la mano para revisar qué falta por enviar
                    IconButton(onClick = onOpenSyncStatus) {
                        Icon(
                            imageVector = AppIcons.Upload,
                            contentDescription = stringResource(Res.string.sync_status_open),
                            tint = AppTheme.colors.title,
                        )
                    }
                },
            )

            PendingSyncBanner(syncState = syncState, onClick = onOpenSyncStatus)

            if (state.isOffline) {
                Spacer(Modifier.height(dimens.spaceS))
                OfflineBanner(stringResource(Res.string.assigned_offline), lastUpdatedAtMillis = state.lastUpdatedAtMillis)
            }
            state.error?.let {
                Spacer(Modifier.height(dimens.spaceS))
                ErrorText(it.asText())
            }
            Spacer(Modifier.height(dimens.spaceM))

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(AssignedVehiclesEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isInitialLoading -> LoadingView()
                    state.vehicles.isEmpty() && state.activeTrip == null ->
                        EmptyView(stringResource(Res.string.assigned_empty))
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = dimens.spaceXl),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
                    ) {
                        state.activeTrip?.let { trip ->
                            item(key = "active", span = { GridItemSpan(maxLineSpan) }) {
                                ActiveTripCard(trip = trip, onContinue = onOpenActiveTrip)
                            }
                        }
                        items(state.vehicles, key = { it.reservationId }) { vehicle ->
                            AssignedVehicleCard(
                                vehicle = vehicle,
                                action = {
                                    // "Comenzar viaje" a lo ancho + botón rojo cuadrado para dejar el vehículo
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AppButton(
                                            text = stringResource(Res.string.assigned_start),
                                            onClick = {
                                                if (state.activeTrip == null) onStartTrip(vehicle.reservationId) else onOpenActiveTrip()
                                            },
                                            variant = AppButtonVariant.Primary,
                                            modifier = Modifier.weight(1f),
                                        )
                                        ReleaseIconButton(
                                            block = state.releaseBlockOf(vehicle),
                                            isOffline = state.isOffline,
                                            onRelease = { onEvent(AssignedVehiclesEvent.ReleaseClicked(vehicle)) },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    ReleaseDialogs(state = state, onEvent = onEvent)
}

/**
 * Aviso "N registros sin enviar · Ver" (o "rechazados") cuando hay algo de días o viajes anteriores
 * que no ha llegado al servidor. No se muestra para el viaje en curso (eso es normal).
 */
@Composable
private fun PendingSyncBanner(syncState: SyncStatusUiState, onClick: () -> Unit) {
    val status = syncState.status
    val pending = syncState.pendingCount
    if (pending == 0 && !status.needsAttention) return
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val attention = status.needsAttention
    Spacer(Modifier.height(dimens.spaceS))
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radius),
        color = if (attention) colors.danger.copy(alpha = 0.12f) else colors.labelBackground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimens.spaceM, vertical = dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (attention) AppIcons.Warning else AppIcons.Upload,
                contentDescription = null,
                tint = if (attention) colors.danger else colors.onLabel,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(dimens.spaceS))
            Text(
                text = if (attention) {
                    stringResource(Res.string.sync_status_banner_attention)
                } else {
                    stringResource(Res.string.sync_status_banner, pending)
                },
                style = AppTheme.typography.bodySecondary,
                color = if (attention) colors.danger else colors.onLabel,
            )
        }
    }
}

/**
 * Botón rojo cuadrado (ícono de salida) para "Dejar vehículo", del alto de "Comenzar viaje".
 * Si todavía no se puede (viaje en curso o sin enviar, o sin red) se ve apagado y al tocarlo
 * explica el motivo en lugar de abrir la confirmación.
 */
@Composable
private fun ReleaseIconButton(block: ReleaseBlock?, isOffline: Boolean, onRelease: () -> Unit) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    var showReason by remember { mutableStateOf(false) }
    val reason = when {
        block == ReleaseBlock.ACTIVE_TRIP -> stringResource(Res.string.assigned_release_blocked_active)
        block == ReleaseBlock.PENDING_SYNC -> stringResource(Res.string.assigned_release_blocked_pending)
        isOffline -> stringResource(Res.string.assigned_release_blocked_offline)
        else -> null
    }
    val enabled = reason == null
    val shape = RoundedCornerShape(dimens.radius)
    Box(
        modifier = Modifier
            .size(dimens.buttonHeight)
            .clip(shape)
            .background(colors.danger.copy(alpha = if (enabled) 0.15f else 0.06f))
            .clickable(role = Role.Button) { if (enabled) onRelease() else showReason = true },
        contentAlignment = Alignment.Center,
    ) {
        // Bote de basura: "quitar este carro de mi lista" se entiende mejor que el ícono de salir
        Icon(
            imageVector = AppIcons.Trash,
            contentDescription = stringResource(Res.string.assigned_release),
            tint = colors.danger.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(dimens.iconSize),
        )
    }

    if (showReason && reason != null) {
        ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.assigned_release_blocked_title),
            message = reason,
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = { showReason = false },
            onDismiss = { showReason = false },
        )
    }
}

@Composable
private fun ReleaseDialogs(state: AssignedVehiclesUiState, onEvent: (AssignedVehiclesEvent) -> Unit) {
    val dismiss = { onEvent(AssignedVehiclesEvent.DialogDismissed) }

    state.releaseTarget?.let { vehicle -> ReleaseConfirmDialog(vehicle = vehicle, state = state, onEvent = onEvent) }

    state.released?.let { vehicle ->
        ResultDialog(
            type = ResultType.Success,
            title = stringResource(Res.string.assigned_release_done_title),
            message = stringResource(Res.string.assigned_release_done_message, vehicle.title, vehicle.plates),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = dismiss,
            onDismiss = dismiss,
        )
    }

    // 409 de la API (ej. un viaje en curso registrado desde otro celular): se muestra su mensaje
    state.releaseError?.let { error ->
        ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.assigned_release_error_title),
            message = error.asText(),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = dismiss,
            onDismiss = dismiss,
        )
    }
}

/** "¿Dejar el Nissan Versa (ABC-123)?" + observaciones opcionales. */
@Composable
private fun ReleaseConfirmDialog(vehicle: AssignedVehicle, state: AssignedVehiclesUiState, onEvent: (AssignedVehiclesEvent) -> Unit) {
    // Mismo diseño que "Enviar solicitud" (hoja desde abajo), con el botón en rojo
    EditSheet(
        title = stringResource(Res.string.assigned_release_sheet_title),
        subtitle = stringResource(Res.string.assigned_release_info),
        cancelText = stringResource(Res.string.action_cancel),
        saveText = stringResource(Res.string.assigned_release),
        onCancel = { onEvent(AssignedVehiclesEvent.DialogDismissed) },
        onSave = { onEvent(AssignedVehiclesEvent.ReleaseConfirmed) },
        isSaving = state.isReleasing,
        closeDescription = stringResource(Res.string.cd_close),
        destructive = true,
    ) {
        // El vehículo que se va a dejar
        SheetSubjectCard(
            title = vehicle.title,
            subtitle = vehicle.plates,
            imageModel = vehicle.photoUrl,
            fallbackIcon = AppIcons.vehicleType(vehicle.vehicleType),
        )
        Spacer(Modifier.height(AppTheme.dimens.spaceL))

        AppTextField(
            value = state.releaseNotes,
            onValueChange = { onEvent(AssignedVehiclesEvent.ReleaseNotesChanged(it)) },
            label = stringResource(Res.string.assigned_release_notes),
            placeholder = stringResource(Res.string.assigned_release_notes_hint),
            modifier = Modifier.fillMaxWidth(),
            errorMessage = state.releaseNotesError?.asText(),
            enabled = !state.isReleasing,
            singleLine = false,
            minLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        SheetCharCounter(
            length = state.releaseNotes.length,
            max = ReleaseVehicleUseCase.MAX_NOTES,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

/** Aviso del viaje que sigue en curso (por ejemplo si la app se cerró). */
@Composable
private fun ActiveTripCard(trip: RecordedTrip, onContinue: () -> Unit) {
    val dimens = AppTheme.dimens
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onContinue) {
        Column(Modifier.fillMaxWidth().padding(dimens.spaceXs)) {
            BodyText(stringResource(Res.string.assigned_active_title))
            SecondaryText("${trip.vehicle.title} • ${TripFormat.km(trip.distanceMeters)} km")
            Spacer(Modifier.height(dimens.spaceS))
            PrimaryButton(
                text = stringResource(Res.string.assigned_active_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = AppIcons.Route,
            )
        }
    }
}
