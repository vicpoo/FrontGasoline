package org.vicpoo.gasolineproject.feature.vehicles.presentation.list

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.action_add_vehicle
import gasolineproject.shared.generated.resources.cd_clear_search
import gasolineproject.shared.generated.resources.vehicle_list_empty
import gasolineproject.shared.generated.resources.vehicle_list_no_results
import gasolineproject.shared.generated.resources.vehicle_list_offline
import gasolineproject.shared.generated.resources.vehicle_list_pending
import gasolineproject.shared.generated.resources.vehicle_list_search
import gasolineproject.shared.generated.resources.vehicle_list_title
import gasolineproject.shared.generated.resources.vehicle_plates_value
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.AppFab
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.components.AppSearchField
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OfflineBanner
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.toImageModel
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.PendingSyncChip
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.SyncErrorChip
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.VehicleStatusChip

@Composable
fun VehicleListRoute(
    onVehicleClick: (String) -> Unit,
    onAddVehicle: () -> Unit,
    viewModel: VehicleListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VehicleListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onVehicleClick = onVehicleClick,
        onAddVehicle = onAddVehicle,
    )
}

/** Figma: "Vehiculos" — buscador, tarjetas y botón "+" (solo con privilegio). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    state: VehicleListUiState,
    onEvent: (VehicleListEvent) -> Unit,
    onVehicleClick: (String) -> Unit,
    onAddVehicle: () -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            if (state.canAdd) {
                AppFab(onClick = onAddVehicle, contentDescription = stringResource(Res.string.action_add_vehicle))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = dimens.maxContentWidthWide)
                    .padding(horizontal = dimens.screenPadding),
            ) {
                Spacer(Modifier.height(dimens.spaceM))
                TitleText(stringResource(Res.string.vehicle_list_title))
                Spacer(Modifier.height(dimens.spaceS))
                AppSearchField(
                    value = state.query,
                    onValueChange = { onEvent(VehicleListEvent.QueryChanged(it)) },
                    placeholder = stringResource(Res.string.vehicle_list_search),
                    clearDescription = stringResource(Res.string.cd_clear_search),
                )

                SyncBanner(state)
                state.error?.let {
                    Spacer(Modifier.height(dimens.spaceS))
                    ErrorText(it.asText())
                }
                Spacer(Modifier.height(dimens.spaceM))

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(VehicleListEvent.Refresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        state.isInitialLoading -> LoadingView()
                        state.vehicles.isEmpty() -> EmptyView(
                            stringResource(
                                if (state.query.isBlank()) Res.string.vehicle_list_empty else Res.string.vehicle_list_no_results,
                            ),
                        )
                        else -> VehicleGrid(state.vehicles, onVehicleClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncBanner(state: VehicleListUiState) {
    val text = when {
        state.isOffline -> stringResource(Res.string.vehicle_list_offline)
        state.pendingSyncCount > 0 -> stringResource(Res.string.vehicle_list_pending, state.pendingSyncCount)
        else -> null
    } ?: return
    Spacer(Modifier.height(AppTheme.dimens.spaceS))
    OfflineBanner(text)
}

/** Adaptativo: 1 columna en teléfono, 2-3 en tablet. */
@Composable
private fun VehicleGrid(vehicles: List<Vehicle>, onVehicleClick: (String) -> Unit) {
    val dimens = AppTheme.dimens
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp), // espacio para el botón "+"
        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        items(vehicles, key = { it.id }) { vehicle ->
            VehicleCard(vehicle = vehicle, onClick = { onVehicleClick(vehicle.id) })
        }
    }
}

/** Tarjeta de vehículo (Figma): foto, "Vocho Amarillo", "Volkswagen Sedán", placas y estado. */
@Composable
fun VehicleCard(vehicle: Vehicle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppImage(
                model = vehicle.photo?.toImageModel(),
                contentDescription = vehicle.cardTitle,
                modifier = Modifier
                    .size(width = 96.dp, height = 72.dp)
                    .clip(RoundedCornerShape(dimens.radiusS)),
            )
            Spacer(Modifier.width(dimens.spaceS))
            Column(Modifier.weight(1f)) {
                Text(
                    text = vehicle.cardTitle,
                    style = AppTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = vehicle.brandAndModel,
                    style = AppTheme.typography.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.vehicle_plates_value, vehicle.plates),
                    style = AppTheme.typography.caption,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(dimens.spaceXs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        vehicle.hasSyncError -> SyncErrorChip()
                        vehicle.isPendingSync -> PendingSyncChip()
                    }
                    Spacer(Modifier.weight(1f))
                    VehicleStatusChip(vehicle.status)
                }
            }
        }
    }
}
