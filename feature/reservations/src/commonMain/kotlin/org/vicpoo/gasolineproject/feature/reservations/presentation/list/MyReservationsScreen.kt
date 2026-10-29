package org.vicpoo.gasolineproject.feature.reservations.presentation.list

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OfflineBanner
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationStatus
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.MyReservationCard
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.Overline
import org.vicpoo.gasolineproject.feature.reservations.resources.Res
import org.vicpoo.gasolineproject.feature.reservations.resources.month_1
import org.vicpoo.gasolineproject.feature.reservations.resources.month_10
import org.vicpoo.gasolineproject.feature.reservations.resources.month_11
import org.vicpoo.gasolineproject.feature.reservations.resources.month_12
import org.vicpoo.gasolineproject.feature.reservations.resources.month_2
import org.vicpoo.gasolineproject.feature.reservations.resources.month_3
import org.vicpoo.gasolineproject.feature.reservations.resources.month_4
import org.vicpoo.gasolineproject.feature.reservations.resources.month_5
import org.vicpoo.gasolineproject.feature.reservations.resources.month_6
import org.vicpoo.gasolineproject.feature.reservations.resources.month_7
import org.vicpoo.gasolineproject.feature.reservations.resources.month_8
import org.vicpoo.gasolineproject.feature.reservations.resources.month_9
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_empty
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_empty_filter
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_filter_all
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_filter_approved
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_filter_pending
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_filter_rejected
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_no_date
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_offline
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_title

private const val AUTO_REFRESH_MS = 30_000L

@Composable
fun MyReservationsRoute(
    onBack: () -> Unit,
    onRequestClick: (String) -> Unit,
    viewModel: MyReservationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Se actualiza al entrar y cada 30 s mientras está en pantalla (sin el indicador)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.refreshSilently()
                delay(AUTO_REFRESH_MS)
            }
        }
    }
    MyReservationsScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack, onRequestClick = onRequestClick)
}

/**
 * Campana: "Mis solicitudes". Filtros con conteo (Todas / Pendientes / Aprobadas / Denegadas),
 * solicitudes agrupadas por mes y una tarjeta por solicitud con su estado y respuesta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReservationsScreen(
    state: MyReservationsUiState,
    onEvent: (MyReservationsEvent) -> Unit,
    onBack: () -> Unit,
    onRequestClick: (String) -> Unit,
) {
    val dimens = AppTheme.dimens
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = dimens.screenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = dimens.maxContentWidthWide)) {
            Spacer(Modifier.height(dimens.spaceM))
            // Flecha y título en la misma línea
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppBackButton(onClick = onBack)
                Spacer(Modifier.width(dimens.spaceM))
                TitleText(stringResource(Res.string.requests_title))
            }

            Spacer(Modifier.height(dimens.spaceM))
            FilterRow(state, onEvent)
            Spacer(Modifier.height(dimens.spaceS))
            HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.divider)

            if (state.isOffline) {
                Spacer(Modifier.height(dimens.spaceS))
                OfflineBanner(stringResource(Res.string.requests_offline), lastUpdatedAtMillis = state.lastUpdatedAtMillis)
            }
            state.error?.let {
                Spacer(Modifier.height(dimens.spaceS))
                ErrorText(it.asText())
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(MyReservationsEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isInitialLoading -> LoadingView()
                    state.requests.isEmpty() -> EmptyView(stringResource(Res.string.requests_empty))
                    state.groups.isEmpty() -> EmptyView(stringResource(Res.string.requests_empty_filter))
                    // Teléfono: una columna; tablet: varias tarjetas por fila (los meses ocupan toda la fila)
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 320.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = dimens.spaceL, bottom = dimens.spaceXl),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
                    ) {
                        state.groups.forEachIndexed { index, group ->
                            item(key = "month_${group.key}", span = { GridItemSpan(maxLineSpan) }) {
                                MonthHeader(group, Modifier.padding(top = if (index == 0) 0.dp else dimens.spaceS))
                            }
                            items(group.requests, key = { it.id }) { request ->
                                // Las que aún no llegan a la API (sin red / no se pudieron enviar) no tienen detalle
                                val sent = request.status != ReservationStatus.NOT_SENT && request.status != ReservationStatus.SEND_FAILED
                                MyReservationCard(request, onClick = if (sent) ({ onRequestClick(request.id) }) else null)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Filtros ----------------

@Composable
private fun FilterRow(state: MyReservationsUiState, onEvent: (MyReservationsEvent) -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.spaceS),
    ) {
        MyReservationsFilter.entries.forEach { filter ->
            val count = state.counts[filter] ?: 0
            // Los filtros vacíos se esconden (menos "Todas" y el que esté elegido)
            if (count == 0 && filter != MyReservationsFilter.ALL && filter != state.filter) return@forEach
            CountChip(
                text = stringResource(filter.label()),
                count = count,
                countColor = when (filter) {
                    MyReservationsFilter.APPROVED -> colors.primary
                    MyReservationsFilter.REJECTED -> colors.danger
                    else -> colors.textSecondary
                },
                selected = state.filter == filter,
                onClick = { onEvent(MyReservationsEvent.FilterSelected(filter)) },
            )
        }
    }
}

private fun MyReservationsFilter.label(): StringResource = when (this) {
    MyReservationsFilter.ALL -> Res.string.requests_filter_all
    MyReservationsFilter.PENDING -> Res.string.requests_filter_pending
    MyReservationsFilter.APPROVED -> Res.string.requests_filter_approved
    MyReservationsFilter.REJECTED -> Res.string.requests_filter_rejected
}

/** Chip "Aprobadas (1)": el número va del color del estado (blanco si está elegido). */
@Composable
private fun CountChip(text: String, count: Int, countColor: Color, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text,
                    style = AppTheme.typography.bodySecondary.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "($count)",
                    style = AppTheme.typography.bodySecondary.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) colors.onPrimary else countColor,
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
            containerColor = colors.background,
            labelColor = colors.text,
        ),
    )
}

// ---------------- Mes ----------------

private val MONTHS = listOf(
    Res.string.month_1, Res.string.month_2, Res.string.month_3, Res.string.month_4,
    Res.string.month_5, Res.string.month_6, Res.string.month_7, Res.string.month_8,
    Res.string.month_9, Res.string.month_10, Res.string.month_11, Res.string.month_12,
)

/** "OCTUBRE 2026 ─────────────" */
@Composable
private fun MonthHeader(group: ReservationMonthGroup, modifier: Modifier = Modifier) {
    val month = group.month?.takeIf { it in 1..12 }
    val label = if (month != null && group.year != null) {
        "${stringResource(MONTHS[month - 1])} ${group.year}"
    } else {
        stringResource(Res.string.requests_no_date)
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Overline(label)
        Spacer(Modifier.width(AppTheme.dimens.spaceM))
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = AppTheme.colors.divider)
    }
}
