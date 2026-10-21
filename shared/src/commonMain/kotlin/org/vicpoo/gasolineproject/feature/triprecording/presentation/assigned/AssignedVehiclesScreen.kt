package org.vicpoo.gasolineproject.feature.triprecording.presentation.assigned

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.assigned_active_continue
import gasolineproject.shared.generated.resources.assigned_active_title
import gasolineproject.shared.generated.resources.assigned_empty
import gasolineproject.shared.generated.resources.assigned_offline
import gasolineproject.shared.generated.resources.assigned_start
import gasolineproject.shared.generated.resources.assigned_subtitle
import gasolineproject.shared.generated.resources.assigned_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.ui.components.AppButton
import org.vicpoo.gasolineproject.core.ui.components.AppButtonVariant
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.BodyText
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OfflineBanner
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.ScreenHeader
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.triprecording.domain.RecordedTrip
import org.vicpoo.gasolineproject.feature.triprecording.presentation.common.AssignedVehicleCard
import org.vicpoo.gasolineproject.feature.triprecording.presentation.common.TripFormat

@Composable
fun AssignedVehiclesRoute(
    onStartTrip: (reservationId: String) -> Unit,
    onOpenActiveTrip: () -> Unit,
    viewModel: AssignedVehiclesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AssignedVehiclesScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onStartTrip = onStartTrip,
        onOpenActiveTrip = onOpenActiveTrip,
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
) {
    val dimens = AppTheme.dimens
    Box(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = dimens.screenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = dimens.maxContentWidthWide)) {
            ScreenHeader(
                title = stringResource(Res.string.assigned_title),
                subtitle = stringResource(Res.string.assigned_subtitle),
            )

            if (state.isOffline) {
                Spacer(Modifier.height(dimens.spaceS))
                OfflineBanner(stringResource(Res.string.assigned_offline))
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
                                    AppButton(
                                        text = stringResource(Res.string.assigned_start),
                                        onClick = {
                                            if (state.activeTrip == null) onStartTrip(vehicle.reservationId) else onOpenActiveTrip()
                                        },
                                        leadingIcon = AppIcons.Play,
                                        variant = AppButtonVariant.Primary,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
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
