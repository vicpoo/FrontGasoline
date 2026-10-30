package org.vicpoo.gasolineproject.feature.vehicles.presentation.list

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.core.ui.components.AppAddButton
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.AppSearchBar
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OfflineBanner
import org.vicpoo.gasolineproject.core.ui.components.PendingSyncChip
import org.vicpoo.gasolineproject.core.ui.components.ScreenHeader
import org.vicpoo.gasolineproject.core.ui.components.SyncErrorChip
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.VehicleStatusChip
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.icon
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.label
import org.vicpoo.gasolineproject.feature.vehicles.resources.Res
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_add_vehicle
import org.vicpoo.gasolineproject.feature.vehicles.resources.cd_clear_filters
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_list_empty
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_list_no_results
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_list_offline
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_list_pending
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_list_search
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_list_title
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_plates_value

@Composable
fun VehicleListRoute(
    mode: VehiclesMode,
    onVehicleClick: (String) -> Unit,
    onAddVehicle: () -> Unit,
    /** Botón de la esquina superior derecha (en Inicio: la campana de "Mis solicitudes"). */
    topEndAction: (@Composable () -> Unit)? = null,
    viewModel: VehicleListViewModel = koinViewModel(key = "vehicle-list-$mode") { parametersOf(VehicleListArgs(mode)) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VehicleListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onVehicleClick = onVehicleClick,
        onAddVehicle = onAddVehicle,
        topEndAction = topEndAction,
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
    topEndAction: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Scaffold(
        containerColor = colors.background,
        // "Agregar vehículo": botón propio, centrado abajo
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (state.canAdd) {
                AppAddButton(text = stringResource(Res.string.action_add_vehicle), onClick = onAddVehicle)
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
                ScreenHeader(title = stringResource(Res.string.vehicle_list_title), action = topEndAction)
                Spacer(Modifier.height(dimens.spaceS))
                // Barra compacta tipo Instagram: al tocarla abre la vista de búsqueda y filtros
                // (no abre el teclado aquí)
                AppSearchBar(
                    text = state.filter.query,
                    placeholder = stringResource(Res.string.vehicle_list_search),
                    onClick = { onEvent(VehicleListEvent.SearchOpened) },
                    activeFilters = state.filter.activeFilters,
                    onClear = { onEvent(VehicleListEvent.FilterCleared) },
                    clearDescription = stringResource(Res.string.cd_clear_filters),
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
                                if (state.filter.isEmpty) Res.string.vehicle_list_empty else Res.string.vehicle_list_no_results,
                            ),
                        )
                        else -> VehicleGrid(state.vehicles, onVehicleClick)
                    }
                }
            }
        }
    }

    state.search?.let { search ->
        VehicleSearchView(search = search, onEvent = onEvent, onVehicleClick = onVehicleClick)
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
    OfflineBanner(text, lastUpdatedAtMillis = state.lastUpdatedAtMillis.takeIf { state.isOffline })
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

/**
 * Tarjeta de vehículo: "Vocho Amarillo", "Volkswagen Sedán", placas y estado.
 * Sin foto (la foto solo se ve en el detalle); a la izquierda va el ícono del carro.
 */
@Composable
fun VehicleCard(vehicle: Vehicle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(vehicle.type.icon, contentDescription = vehicle.type.label(), tint = colors.primary, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(dimens.spaceM))
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
