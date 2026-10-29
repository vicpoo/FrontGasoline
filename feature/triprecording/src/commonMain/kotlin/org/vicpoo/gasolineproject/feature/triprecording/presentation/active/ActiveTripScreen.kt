package org.vicpoo.gasolineproject.feature.triprecording.presentation.active

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.location.rememberLocationPermission
import org.vicpoo.gasolineproject.core.ui.adaptive.LocalWindowWidth
import org.vicpoo.gasolineproject.core.ui.adaptive.WindowWidth
import org.vicpoo.gasolineproject.core.ui.adaptive.isLandscapePhone
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.ConfirmDialog
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.PendingSyncChip
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.ResultDialog
import org.vicpoo.gasolineproject.core.ui.components.ResultType
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.SyncErrorChip
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.map.LiveRouteMap
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecord
import org.vicpoo.gasolineproject.feature.triprecording.domain.RecordSyncState
import org.vicpoo.gasolineproject.feature.triprecording.presentation.common.TripFormat
import org.vicpoo.gasolineproject.feature.triprecording.resources.Res
import org.vicpoo.gasolineproject.feature.triprecording.resources.action_no
import org.vicpoo.gasolineproject.feature.triprecording.resources.action_understood
import org.vicpoo.gasolineproject.feature.triprecording.resources.action_yes
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_action_error_title
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_add_fuel
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_date
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_delete_confirm
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_distance
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_duration
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_finish
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_finish_confirm
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_fuel_detail
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_fuel_item
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_gps_no_permission
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_gps_on
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_gps_searching
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_no_fuel
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_none
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_pause
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_paused
import org.vicpoo.gasolineproject.feature.triprecording.resources.active_trip_resume
import org.vicpoo.gasolineproject.feature.triprecording.resources.cd_delete_fuel
import org.vicpoo.gasolineproject.feature.triprecording.resources.cd_edit_fuel
import org.vicpoo.gasolineproject.feature.triprecording.resources.cd_finish_trip
import org.vicpoo.gasolineproject.feature.triprecording.resources.cd_pause_trip
import org.vicpoo.gasolineproject.feature.triprecording.resources.cd_resume_trip

@Composable
fun ActiveTripRoute(
    onBack: () -> Unit,
    onAddFuel: () -> Unit,
    onEditFuel: (recordId: String) -> Unit,
    onOpenFinish: () -> Unit,
    viewModel: ActiveTripViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberLocationPermission { viewModel.onEvent(ActiveTripEvent.PermissionChanged) }

    // "Sí" en terminar (o la app se cerró en "Registrar Viaje"): se abre la pantalla del kilometraje final
    LaunchedEffect(state.navigateToFinish) {
        if (state.navigateToFinish) {
            viewModel.onEvent(ActiveTripEvent.NavigationHandled)
            onOpenFinish()
        }
    }
    ActiveTripScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onAddFuel = onAddFuel,
        onEditFuel = onEditFuel,
        onRequestPermission = { permission.launch() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripScreen(
    state: ActiveTripUiState,
    onEvent: (ActiveTripEvent) -> Unit,
    onBack: () -> Unit,
    onAddFuel: () -> Unit,
    onEditFuel: (String) -> Unit,
    onRequestPermission: () -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        if (state.hasTrip && (isLandscapePhone || LocalWindowWidth.current == WindowWidth.Expanded)) {
            // Teléfono acostado o tablet: la hoja inferior taparía casi todo el mapa.
            // Mapa a la izquierda y panel fijo con los datos, botones y recargas a la derecha.
            Row(Modifier.fillMaxSize()) {
                TripMap(state = state, onBack = onBack, onRequestPermission = onRequestPermission, modifier = Modifier.weight(1f))
                Surface(
                    color = colors.background,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(SIDE_PANEL_WIDTH).fillMaxHeight(),
                ) {
                    TripSheet(
                        state = state,
                        onEvent = onEvent,
                        onAddFuel = onAddFuel,
                        onEditFuel = onEditFuel,
                        modifier = Modifier.statusBarsPadding().padding(top = dimens.spaceM),
                    )
                }
            }
        } else if (state.hasTrip) {
            // Hoja inferior de Material 3: se desliza hacia abajo para ver más mapa (queda visible
            // la duración, distancia y fecha) y hacia arriba para ver los botones y las recargas.
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded),
            )
            val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val maxSheetHeight = maxHeight * 0.6f

            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = SHEET_PEEK_HEIGHT + navigationBarHeight,
                sheetMaxWidth = dimens.maxContentWidthWide,
                sheetShape = RoundedCornerShape(topStart = dimens.spaceL, topEnd = dimens.spaceL),
                sheetContainerColor = colors.background,
                sheetShadowElevation = 8.dp,
                sheetDragHandle = { BottomSheetDefaults.DragHandle(color = colors.labelBackground) },
                containerColor = colors.background,
                sheetContent = {
                    TripSheet(
                        state = state,
                        onEvent = onEvent,
                        onAddFuel = onAddFuel,
                        onEditFuel = onEditFuel,
                        modifier = Modifier.heightIn(max = maxSheetHeight),
                    )
                },
            ) { _ ->
                // El mapa ocupa toda la pantalla (la hoja va encima); por eso no se usa el padding
                TripMap(state = state, onBack = onBack, onRequestPermission = onRequestPermission, modifier = Modifier.fillMaxSize())
            }
        } else if (state.isReady) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(dimens.screenPadding)) {
                AppBackButton(onClick = onBack)
                EmptyView(stringResource(Res.string.active_trip_none))
            }
        }
    }

    // ---- Diálogos ----
    if (state.showFinishConfirm) {
        ConfirmDialog(
            message = stringResource(Res.string.active_trip_finish_confirm),
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = { onEvent(ActiveTripEvent.FinishConfirmed) },
            onDismiss = { onEvent(ActiveTripEvent.FinishDismissed) },
            isLoading = state.isStopping,
        )
    }

    state.error?.let { error ->
        ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.active_trip_action_error_title),
            message = error.asText(),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = { onEvent(ActiveTripEvent.ErrorDismissed) },
            onDismiss = { onEvent(ActiveTripEvent.ErrorDismissed) },
        )
    }

    state.deleteCandidate?.let {
        ConfirmDialog(
            message = stringResource(Res.string.active_trip_delete_confirm),
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = { onEvent(ActiveTripEvent.DeleteConfirmed) },
            onDismiss = { onEvent(ActiveTripEvent.DeleteDismissed) },
            isLoading = state.isDeleting,
        )
    }
}

/** Ancho del panel de controles cuando el mapa va a la izquierda (teléfono acostado / tablet). */
private val SIDE_PANEL_WIDTH = 380.dp

/** Mapa con la ruta + arriba el botón de regresar y el aviso del GPS. */
@Composable
private fun TripMap(
    state: ActiveTripUiState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val session = state.session
    Box(modifier.fillMaxHeight()) {
        LiveRouteMap(route = session.route, current = session.current, modifier = Modifier.fillMaxSize())

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
                onRequestPermission = onRequestPermission,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** "Señal de Gps activa" (azul) / en pausa / buscando o sin permiso (rojo). */
@Composable
private fun GpsBanner(
    gpsActive: Boolean,
    isPaused: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val (text, container, content) = when {
        isPaused -> Triple(stringResource(Res.string.active_trip_paused), colors.secondary, colors.onSecondary)
        !hasPermission -> Triple(stringResource(Res.string.active_trip_gps_no_permission), colors.danger, colors.onDanger)
        gpsActive -> Triple(stringResource(Res.string.active_trip_gps_on), colors.primary, colors.onPrimary)
        else -> Triple(stringResource(Res.string.active_trip_gps_searching), colors.secondary, colors.onSecondary)
    }
    Surface(
        modifier = modifier.then(if (!hasPermission) Modifier.clickable(onClick = onRequestPermission) else Modifier),
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

/**
 * Alto de la hoja cuando se desliza hacia abajo: agarradera + duración, distancia y fecha.
 * (Se le suma la barra de navegación del sistema.)
 */
private val SHEET_PEEK_HEIGHT = 100.dp

/**
 * Contenido de la hoja inferior: estadísticas (siempre visibles), pausa / finalizar,
 * "registrar recarga" y lista de recargas. La agarradera la pone BottomSheetScaffold.
 */
@Composable
private fun TripSheet(
    state: ActiveTripUiState,
    onEvent: (ActiveTripEvent) -> Unit,
    onAddFuel: () -> Unit,
    onEditFuel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = AppTheme.dimens
    val trip = state.session.trip ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = dimens.screenPadding, end = dimens.screenPadding, bottom = dimens.spaceM),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(stringResource(Res.string.active_trip_duration), TripFormat.duration(state.elapsedMillis))
            Stat(stringResource(Res.string.active_trip_distance), TripFormat.km(trip.distanceMeters))
            Stat(stringResource(Res.string.active_trip_date), TripFormat.dayMonth(trip.startedAt))
        }
        Spacer(Modifier.height(dimens.spaceM))

        // Pausa / reanudar a la izquierda y el botón grande ■ de terminar (Figma)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PauseButton(
                    isPaused = state.isPaused,
                    isLoading = state.isChangingPause,
                    onClick = {
                        onEvent(if (state.isPaused) ActiveTripEvent.ResumeClicked else ActiveTripEvent.PauseClicked)
                    },
                )
                Spacer(Modifier.height(dimens.spaceXs))
                SecondaryText(
                    stringResource(if (state.isPaused) Res.string.active_trip_resume else Res.string.active_trip_pause),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FinishButton(isLoading = state.isStopping, onClick = { onEvent(ActiveTripEvent.FinishClicked) })
                Spacer(Modifier.height(dimens.spaceXs))
                SecondaryText(stringResource(Res.string.active_trip_finish))
            }
        }
        Spacer(Modifier.height(dimens.spaceM))

        if (state.fuel.canAdd) {
            PrimaryButton(
                text = stringResource(Res.string.active_trip_add_fuel),
                onClick = onAddFuel,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = AppIcons.Fuel,
            )
            Spacer(Modifier.height(dimens.spaceM))
        }

        if (state.fuelRecords.isEmpty()) {
            SecondaryText(stringResource(Res.string.active_trip_no_fuel), textAlign = TextAlign.Center)
        } else {
            state.fuelRecords.forEach { record ->
                FuelRecordRow(
                    record = record,
                    canEdit = state.fuel.canEdit,
                    canDelete = state.fuel.canDelete,
                    onEdit = { onEditFuel(record.localId) },
                    onDelete = { onEvent(ActiveTripEvent.DeleteClicked(record)) },
                )
                Spacer(Modifier.height(dimens.spaceS))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = AppTheme.typography.caption, color = AppTheme.colors.textSecondary)
        Text(
            value,
            style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.Medium),
            color = AppTheme.colors.title,
        )
    }
}

/** Botón redondo para pausar (‖) o reanudar (▷) el viaje. */
@Composable
private fun PauseButton(isPaused: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = CircleShape,
        color = colors.secondary,
        shadowElevation = 4.dp,
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(color = colors.onSecondary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    if (isPaused) AppIcons.Play else AppIcons.Pause,
                    contentDescription = stringResource(if (isPaused) Res.string.cd_resume_trip else Res.string.cd_pause_trip),
                    tint = colors.onSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

/** Botón redondo grande (Figma) para terminar el viaje. */
@Composable
private fun FinishButton(isLoading: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = CircleShape,
        color = colors.primary,
        shadowElevation = 4.dp,
        modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            } else {
                Icon(
                    AppIcons.Stop,
                    contentDescription = stringResource(Res.string.cd_finish_trip),
                    tint = colors.onPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/** "Recarga 1#" con editar y eliminar. */
@Composable
private fun FuelRecordRow(
    record: FuelRecord,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radius),
        color = colors.labelBackground,
    ) {
        Column(Modifier.padding(start = dimens.spaceM, end = dimens.spaceXs, top = dimens.spaceXs, bottom = dimens.spaceS)) {
            // Fila: ícono · "Recarga 1#" y "$2,000 • 25 L" · editar · eliminar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Fuel, contentDescription = null, tint = colors.onLabel, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(dimens.spaceS))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.active_trip_fuel_item, record.number),
                        style = AppTheme.typography.body,
                        color = colors.onLabel,
                        maxLines = 1,
                    )
                    Text(
                        stringResource(
                            Res.string.active_trip_fuel_detail,
                            TripFormat.money(record.amount),
                            TripFormat.number(record.liters),
                        ),
                        style = AppTheme.typography.caption,
                        color = colors.onLabel,
                        maxLines = 1,
                    )
                }
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(AppIcons.Pencil, contentDescription = stringResource(Res.string.cd_edit_fuel), tint = colors.onLabel)
                    }
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(AppIcons.Trash, contentDescription = stringResource(Res.string.cd_delete_fuel), tint = colors.danger)
                    }
                }
            }
            // Estado de envío y el motivo del error, debajo y a todo lo ancho (así no aprietan el texto)
            if (record.syncState != RecordSyncState.SYNCED) {
                Spacer(Modifier.height(dimens.spaceXs))
                when (record.syncState) {
                    RecordSyncState.PENDING -> PendingSyncChip()
                    RecordSyncState.ERROR -> SyncErrorChip()
                    RecordSyncState.SYNCED -> Unit
                }
                record.syncErrorMessage?.let {
                    Spacer(Modifier.height(dimens.spaceXs))
                    Text(it, style = AppTheme.typography.caption, color = colors.error)
                }
            }
        }
    }
}
