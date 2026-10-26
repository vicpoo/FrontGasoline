package org.vicpoo.gasolineproject.feature.trips.presentation.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.trip_history_empty
import gasolineproject.shared.generated.resources.trip_history_offline
import gasolineproject.shared.generated.resources.trip_history_title
import gasolineproject.shared.generated.resources.trip_km_value
import gasolineproject.shared.generated.resources.trip_records_value
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.common.AppDates
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OfflineBanner
import org.vicpoo.gasolineproject.core.ui.components.RouteMap
import org.vicpoo.gasolineproject.core.ui.components.ScreenHeader
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.trips.domain.Trip
import org.vicpoo.gasolineproject.feature.trips.presentation.common.OperatorHeader
import org.vicpoo.gasolineproject.core.common.formatDecimal

@Composable
fun TripHistoryRoute(
    onTripClick: (String) -> Unit,
    viewModel: TripHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Se vuelve a pedir el historial cada vez que la pestaña se muestra (al cambiar de pestaña,
    // al regresar del detalle o al volver a la app), no solo la primera vez que se crea el ViewModel.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshSilently()
        onPauseOrDispose { }
    }
    TripHistoryScreen(state = state, onRefresh = viewModel::refresh, onTripClick = onTripClick)
}

/** Figma: "Historial" — tarjetas con operador, fecha, km, duración, registros y mapa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    state: TripHistoryUiState,
    onRefresh: () -> Unit,
    onTripClick: (String) -> Unit,
) {
    val dimens = AppTheme.dimens
    Box(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.background).statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = dimens.maxContentWidthWide)
                .padding(horizontal = dimens.screenPadding),
        ) {
            ScreenHeader(title = stringResource(Res.string.trip_history_title))
            if (state.isOffline) {
                Spacer(Modifier.height(dimens.spaceS))
                OfflineBanner(stringResource(Res.string.trip_history_offline))
            }
            state.error?.let {
                Spacer(Modifier.height(dimens.spaceS))
                ErrorText(it.asText())
            }
            Spacer(Modifier.height(dimens.spaceM))

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isInitialLoading -> LoadingView()
                    state.trips.isEmpty() -> EmptyView(stringResource(Res.string.trip_history_empty))
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 320.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = dimens.spaceL),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
                    ) {
                        items(state.trips, key = { it.id }) { trip ->
                            TripCard(trip = trip, onClick = { onTripClick(trip.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripCard(trip: Trip, onClick: () -> Unit) {
    val dimens = AppTheme.dimens
    val colors = AppTheme.colors
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(dimens.spaceXs)) {
            OperatorHeader(name = trip.operatorName, photoUrl = trip.operatorPhotoUrl)
            Spacer(Modifier.height(dimens.spaceXs))
            Text(AppDates.longDate(trip.startedAt), style = AppTheme.typography.bodySecondary, color = colors.text)
            Row(Modifier.fillMaxWidth().padding(vertical = dimens.spaceXs), horizontalArrangement = Arrangement.SpaceBetween) {
                val small = AppTheme.typography.caption
                Text(
                    trip.distanceKm?.let { stringResource(Res.string.trip_km_value, formatDecimal(it)) } ?: "—",
                    style = small,
                    color = colors.text,
                )
                // Duración real: sin el tiempo en pausa
                val minutes = AppDates.minutesBetween(trip.startedAt, trip.endedAt)?.let { (it - trip.pausedSeconds / 60).coerceAtLeast(0) }
                Text(AppDates.formatDuration(minutes), style = small, color = colors.text)
                Text(stringResource(Res.string.trip_records_value, trip.fuelLoads.size), style = small, color = colors.text)
            }
            RouteMap(
                points = trip.route,
                modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(dimens.radiusS)),
            )
        }
    }
}
