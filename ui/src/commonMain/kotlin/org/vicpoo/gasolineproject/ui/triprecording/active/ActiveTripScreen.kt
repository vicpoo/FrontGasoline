package org.vicpoo.gasolineproject.ui.triprecording.active

import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.platform.location.rememberLocationPermission
import org.vicpoo.gasolineproject.ui.common.adaptive.LocalWindowWidth
import org.vicpoo.gasolineproject.ui.common.adaptive.WindowWidth
import org.vicpoo.gasolineproject.ui.common.adaptive.isLandscapePhone
import org.vicpoo.gasolineproject.ui.common.components.AppBackButton
import org.vicpoo.gasolineproject.ui.common.components.ConfirmDialog
import org.vicpoo.gasolineproject.ui.common.components.EmptyView
import org.vicpoo.gasolineproject.ui.common.components.ResultDialog
import org.vicpoo.gasolineproject.ui.common.components.ResultType
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.action_no
import org.vicpoo.gasolineproject.ui.resources.action_understood
import org.vicpoo.gasolineproject.ui.resources.action_yes
import org.vicpoo.gasolineproject.ui.resources.active_trip_action_error_title
import org.vicpoo.gasolineproject.ui.resources.active_trip_delete_confirm
import org.vicpoo.gasolineproject.ui.resources.active_trip_finish_confirm
import org.vicpoo.gasolineproject.ui.resources.active_trip_none

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
