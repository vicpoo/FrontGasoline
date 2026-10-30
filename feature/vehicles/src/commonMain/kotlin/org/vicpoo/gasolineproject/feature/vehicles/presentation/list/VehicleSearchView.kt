package org.vicpoo.gasolineproject.feature.vehicles.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.components.AppFilterChip
import org.vicpoo.gasolineproject.core.ui.components.AppTextButton
import org.vicpoo.gasolineproject.core.ui.components.FullScreenSearch
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleStatus
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleType
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.VehicleStatusChip
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.icon
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.label
import org.vicpoo.gasolineproject.feature.vehicles.resources.Res
import org.vicpoo.gasolineproject.feature.vehicles.resources.cd_clear_search
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_list_no_results
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_search_clear_all
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_search_filters
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_search_placeholder
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_search_results
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_search_status
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_search_type

/** Estados que se pueden filtrar (UNKNOWN no se ofrece). */
private val FilterableStatuses = listOf(VehicleStatus.AVAILABLE, VehicleStatus.IN_USE, VehicleStatus.MAINTENANCE)

/**
 * Vista de búsqueda de vehículos (se abre al tocar la barra de la lista), estilo Instagram/Airbnb:
 * - Arriba el campo; el teclado NO se abre solo, solo al tocar el campo.
 * - "Filtros" con "Borrar todo" a la derecha, y las píldoras de Estado y Tipo.
 * - Mientras se escribe o filtra, los resultados en vivo (tocar uno abre su detalle).
 * - Al regresar (‹ o atrás) o con la tecla buscar, la búsqueda y filtros quedan aplicados a la lista.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VehicleSearchView(
    search: VehicleSearchUiState,
    onEvent: (VehicleListEvent) -> Unit,
    onVehicleClick: (String) -> Unit,
) {
    val dimens = AppTheme.dimens
    val draft = search.draft

    FullScreenSearch(
        query = draft.query,
        onQueryChange = { onEvent(VehicleListEvent.QueryChanged(it)) },
        placeholder = stringResource(Res.string.vehicle_search_placeholder),
        onDismiss = { onEvent(VehicleListEvent.SearchDismissed) },
        onSearch = { onEvent(VehicleListEvent.SearchApplied) },
        clearDescription = stringResource(Res.string.cd_clear_search),
        // El teclado solo se abre cuando el usuario toca el campo
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
                        text = stringResource(Res.string.vehicle_search_filters),
                        style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.title,
                        modifier = Modifier.weight(1f),
                    )
                    if (!draft.isEmpty) {
                        AppTextButton(
                            text = stringResource(Res.string.vehicle_search_clear_all),
                            onClick = { onEvent(VehicleListEvent.DraftCleared) },
                        )
                    }
                }
                Spacer(Modifier.height(dimens.spaceS))
            }
            item(key = "status") {
                FilterGroup(title = stringResource(Res.string.vehicle_search_status)) {
                    FilterableStatuses.forEach { status ->
                        AppFilterChip(
                            text = status.label(),
                            selected = status in draft.statuses,
                            onClick = { onEvent(VehicleListEvent.StatusToggled(status)) },
                        )
                    }
                }
            }
            item(key = "type") {
                Spacer(Modifier.height(dimens.spaceL))
                FilterGroup(title = stringResource(Res.string.vehicle_search_type)) {
                    VehicleType.entries.forEach { type ->
                        AppFilterChip(
                            text = type.label(),
                            icon = type.icon,
                            selected = type in draft.types,
                            onClick = { onEvent(VehicleListEvent.TypeToggled(type)) },
                        )
                    }
                }
            }

            // Resultados en vivo solo cuando ya se escribió o filtró algo (si no, la vista se ve limpia)
            if (!draft.isEmpty) {
                item(key = "results-title") {
                    Spacer(Modifier.height(dimens.spaceL))
                    SectionTitle(stringResource(Res.string.vehicle_search_results))
                    Spacer(Modifier.height(dimens.spaceXs))
                }
                if (search.results.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(Res.string.vehicle_list_no_results),
                            style = AppTheme.typography.bodySecondary,
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(vertical = dimens.spaceM),
                        )
                    }
                } else {
                    items(search.results, key = { it.id }) { vehicle ->
                        SearchResultRow(
                            vehicle = vehicle,
                            onClick = {
                                onEvent(VehicleListEvent.SearchDismissed)
                                onVehicleClick(vehicle.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, chips: @Composable () -> Unit) {
    val dimens = AppTheme.dimens
    Column {
        SectionTitle(title)
        Spacer(Modifier.height(dimens.spaceS))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) { chips() }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
        color = AppTheme.colors.title,
    )
}

/** Fila de resultado (como las sugerencias de Instagram): ícono del tipo, nombre y placas. */
@Composable
private fun SearchResultRow(vehicle: Vehicle, onClick: () -> Unit) {
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
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(vehicle.type.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
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
                text = listOf(vehicle.brandAndModel, vehicle.plates).joinToString(" · "),
                style = AppTheme.typography.caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(dimens.spaceS))
        VehicleStatusChip(vehicle.status)
    }
}
