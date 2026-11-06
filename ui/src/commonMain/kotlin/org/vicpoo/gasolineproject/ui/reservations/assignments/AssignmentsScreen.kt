package org.vicpoo.gasolineproject.ui.reservations.assignments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.ui.common.components.AppCard
import org.vicpoo.gasolineproject.ui.common.components.AppFilterChip
import org.vicpoo.gasolineproject.ui.common.components.AppSearchBar
import org.vicpoo.gasolineproject.ui.common.components.AppTextButton
import org.vicpoo.gasolineproject.ui.common.components.EmptyView
import org.vicpoo.gasolineproject.ui.common.components.ErrorText
import org.vicpoo.gasolineproject.ui.common.components.FullScreenSearch
import org.vicpoo.gasolineproject.ui.common.components.LoadingView
import org.vicpoo.gasolineproject.ui.common.components.OfflineBanner
import org.vicpoo.gasolineproject.ui.common.components.ScreenHeader
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.domain.reservations.AssignmentKey
import org.vicpoo.gasolineproject.domain.reservations.EmployeeAssignments
import org.vicpoo.gasolineproject.domain.reservations.VehicleAssignment
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.assignments_empty_employees
import org.vicpoo.gasolineproject.ui.resources.assignments_empty_search
import org.vicpoo.gasolineproject.ui.resources.assignments_empty_vehicles
import org.vicpoo.gasolineproject.ui.resources.assignments_filter_employees
import org.vicpoo.gasolineproject.ui.resources.assignments_filter_vehicles
import org.vicpoo.gasolineproject.ui.resources.assignments_holder_value
import org.vicpoo.gasolineproject.ui.resources.assignments_offline
import org.vicpoo.gasolineproject.ui.resources.assignments_plates_value
import org.vicpoo.gasolineproject.ui.resources.assignments_search_bar
import org.vicpoo.gasolineproject.ui.resources.assignments_search_clear_all
import org.vicpoo.gasolineproject.ui.resources.assignments_search_filters
import org.vicpoo.gasolineproject.ui.resources.assignments_search_placeholder
import org.vicpoo.gasolineproject.ui.resources.assignments_search_results
import org.vicpoo.gasolineproject.ui.resources.assignments_search_view_by
import org.vicpoo.gasolineproject.ui.resources.assignments_title
import org.vicpoo.gasolineproject.ui.resources.assignments_vehicle_count
import org.vicpoo.gasolineproject.ui.resources.cd_clear_filters
import org.vicpoo.gasolineproject.ui.resources.cd_clear_search

/** Pestaña Asignaciones. Al tocar una tarjeta se abre su detalle (ahí está "Desvincular"). */
@Composable
fun AssignmentsRoute(
    onAssignmentClick: (AssignmentKey) -> Unit,
    viewModel: AssignmentsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Se actualiza al entrar (cambiar de pestaña, volver del detalle o volver a la app), sin el indicador
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.refreshSilently() }
    }
    AssignmentsScreen(state = state, onEvent = viewModel::onEvent, onAssignmentClick = onAssignmentClick)
}

/**
 * Vista Asignaciones: SOLO los carros vinculados a un empleado, en tarjetas simples (ícono, nombre,
 * placas y quién lo tiene). Por carro (normal) o por empleado (filtro "Ver por").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    state: AssignmentsUiState,
    onEvent: (AssignmentsEvent) -> Unit,
    onAssignmentClick: (AssignmentKey) -> Unit,
) {
    val dimens = AppTheme.dimens
    Box(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = dimens.screenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = dimens.maxContentWidthWide)) {
            ScreenHeader(title = stringResource(Res.string.assignments_title))
            Spacer(Modifier.height(dimens.spaceS))

            // Igual que Vehículos: barra compacta que abre la vista de búsqueda con los filtros adentro
            AppSearchBar(
                text = state.query,
                placeholder = stringResource(Res.string.assignments_search_bar),
                onClick = { onEvent(AssignmentsEvent.SearchOpened) },
                activeFilters = state.activeFilters,
                onClear = if (state.hasSearchOrFilters) ({ onEvent(AssignmentsEvent.SearchCleared) }) else null,
                clearDescription = stringResource(Res.string.cd_clear_filters),
            )

            if (state.isOffline) {
                Spacer(Modifier.height(dimens.spaceS))
                OfflineBanner(stringResource(Res.string.assignments_offline))
            }
            state.error?.let {
                Spacer(Modifier.height(dimens.spaceS))
                ErrorText(it.asText())
            }
            Spacer(Modifier.height(dimens.spaceM))

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(AssignmentsEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                val visible = state.visible
                val searching = state.query.isNotBlank()
                val byVehicle = state.viewBy == AssignmentsSegment.VEHICLES
                when {
                    state.isInitialLoading -> LoadingView()
                    byVehicle && visible.vehicles.isEmpty() -> EmptyView(
                        stringResource(if (searching) Res.string.assignments_empty_search else Res.string.assignments_empty_vehicles),
                    )
                    !byVehicle && visible.employees.isEmpty() -> EmptyView(
                        stringResource(if (searching) Res.string.assignments_empty_search else Res.string.assignments_empty_employees),
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = dimens.spaceXl),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
                    ) {
                        if (byVehicle) {
                            items(visible.vehicles, key = { it.vehicle.id }) { item ->
                                VehicleAssignmentCard(item = item, onClick = { openVehicle(item, onAssignmentClick) })
                            }
                        } else {
                            items(visible.employees, key = { it.employee.id }) { item ->
                                EmployeeAssignmentsCard(
                                    item = item,
                                    onClick = { onAssignmentClick(AssignmentKey.Employee(item.employee.id)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.isSearchOpen) AssignmentsSearchView(state = state, onEvent = onEvent, onAssignmentClick = onAssignmentClick)
}

private fun openVehicle(item: VehicleAssignment, onAssignmentClick: (AssignmentKey) -> Unit) {
    item.assignment?.let { onAssignmentClick(AssignmentKey.Reservation(it.reservationId)) }
}

// ---------------------------------------------------------------- Tarjetas

/** "Carros": ícono del tipo, "Marca Modelo Año", placas y a quién está asignado. */
@Composable
private fun VehicleAssignmentCard(item: VehicleAssignment, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(item.vehicle.type.icon, contentDescription = null)
            Spacer(Modifier.width(dimens.spaceM))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.vehicle.title,
                    style = AppTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.assignments_plates_value, item.vehicle.plates),
                    style = AppTheme.typography.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(dimens.spaceXs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.holder?.let { holder ->
                        Icon(AppIcons.User, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(dimens.spaceXs))
                        Text(
                            text = stringResource(Res.string.assignments_holder_value, holder.name),
                            style = AppTheme.typography.caption,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } ?: Spacer(Modifier.weight(1f))
                    if (item.assignment?.hasActiveTrip == true) {
                        Spacer(Modifier.width(dimens.spaceS))
                        ActiveTripChip()
                    }
                }
            }
        }
    }
}

/** "Empleados": ícono de persona, nombre, cuántos carros tiene y sus placas. */
@Composable
private fun EmployeeAssignmentsCard(item: EmployeeAssignments, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(AppIcons.User, contentDescription = null)
            Spacer(Modifier.width(dimens.spaceM))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.employee.name,
                    style = AppTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.assignments_vehicle_count, item.vehicles.size),
                    style = AppTheme.typography.caption,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(dimens.spaceXs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.vehicles.joinToString(" · ") { it.vehicle.plates },
                        style = AppTheme.typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.vehicles.any { it.assignment.hasActiveTrip }) {
                        Spacer(Modifier.width(dimens.spaceS))
                        ActiveTripChip()
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Búsqueda

/**
 * Vista de búsqueda (se abre al tocar la barra), igual que la de Vehículos:
 * - Arriba el campo (el teclado solo se abre al tocarlo).
 * - "Filtros" con "Borrar todo" y "Ver por": Carros / Empleados. Ninguno viene elegido: el usuario
 *   lo elige y, si lo vuelve a tocar, se quita.
 * - En cuanto se escribe O se elige un filtro aparecen los resultados en vivo (tocar uno abre su detalle).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssignmentsSearchView(
    state: AssignmentsUiState,
    onEvent: (AssignmentsEvent) -> Unit,
    onAssignmentClick: (AssignmentKey) -> Unit,
) {
    val dimens = AppTheme.dimens
    val colors = AppTheme.colors
    val dismiss = { onEvent(AssignmentsEvent.SearchDismissed) }

    FullScreenSearch(
        query = state.query,
        onQueryChange = { onEvent(AssignmentsEvent.QueryChanged(it)) },
        placeholder = stringResource(Res.string.assignments_search_placeholder),
        onDismiss = dismiss,
        onSearch = dismiss,
        clearDescription = stringResource(Res.string.cd_clear_search),
        autoFocus = false,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceM),
        ) {
            // "Filtros ................ Borrar todo" (el botón solo si hay algo que borrar)
            item(key = "filters-header") {
                Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.assignments_search_filters),
                        style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.title,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.hasSearchOrFilters) {
                        AppTextButton(
                            text = stringResource(Res.string.assignments_search_clear_all),
                            onClick = { onEvent(AssignmentsEvent.SearchCleared) },
                        )
                    }
                }
                Spacer(Modifier.height(dimens.spaceS))
            }
            item(key = "view-by") {
                SearchSectionTitle(stringResource(Res.string.assignments_search_view_by))
                Spacer(Modifier.height(dimens.spaceS))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
                ) {
                    AppFilterChip(
                        text = stringResource(Res.string.assignments_filter_vehicles, state.overview.vehicles.size),
                        icon = AppIcons.Car,
                        selected = state.segment == AssignmentsSegment.VEHICLES,
                        onClick = { onEvent(AssignmentsEvent.SegmentToggled(AssignmentsSegment.VEHICLES)) },
                    )
                    AppFilterChip(
                        text = stringResource(Res.string.assignments_filter_employees, state.overview.employees.size),
                        icon = AppIcons.User,
                        selected = state.segment == AssignmentsSegment.EMPLOYEES,
                        onClick = { onEvent(AssignmentsEvent.SegmentToggled(AssignmentsSegment.EMPLOYEES)) },
                    )
                }
            }

            // Resultados en vivo en cuanto hay texto O un filtro elegido (si no, la vista se ve limpia)
            if (state.hasSearchOrFilters) {
                val visible = state.visible
                val byVehicle = state.viewBy == AssignmentsSegment.VEHICLES
                item(key = "results-title") {
                    Spacer(Modifier.height(dimens.spaceL))
                    SearchSectionTitle(stringResource(Res.string.assignments_search_results))
                    Spacer(Modifier.height(dimens.spaceXs))
                }
                val empty = if (byVehicle) visible.vehicles.isEmpty() else visible.employees.isEmpty()
                if (empty) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(Res.string.assignments_empty_search),
                            style = AppTheme.typography.bodySecondary,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = dimens.spaceM),
                        )
                    }
                } else if (byVehicle) {
                    items(visible.vehicles, key = { "v-" + it.vehicle.id }) { item ->
                        SearchResultRow(
                            title = item.vehicle.title,
                            subtitle = listOfNotNull(item.vehicle.plates, item.holder?.name).joinToString(" · "),
                            leading = { IconBadge(item.vehicle.type.icon, contentDescription = null, size = 40.dp) },
                            onClick = {
                                dismiss()
                                openVehicle(item, onAssignmentClick)
                            },
                        )
                    }
                } else {
                    items(visible.employees, key = { "e-" + it.employee.id }) { item ->
                        SearchResultRow(
                            title = item.employee.name,
                            subtitle = item.vehicles.joinToString(" · ") { it.vehicle.plates },
                            leading = { IconBadge(AppIcons.User, contentDescription = null, size = 40.dp) },
                            onClick = {
                                dismiss()
                                onAssignmentClick(AssignmentKey.Employee(item.employee.id))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
        color = AppTheme.colors.title,
    )
}

/** Fila de resultado (como en Vehículos): ícono, nombre y detalle. */
@Composable
private fun SearchResultRow(title: String, subtitle: String, leading: @Composable () -> Unit, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radius))
            .clickable(onClick = onClick)
            .padding(vertical = dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(dimens.spaceM))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                color = colors.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = AppTheme.typography.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
