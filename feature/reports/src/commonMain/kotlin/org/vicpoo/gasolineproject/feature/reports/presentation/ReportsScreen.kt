package org.vicpoo.gasolineproject.feature.reports.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.common.AppDates
import org.vicpoo.gasolineproject.core.platform.SharedFile
import org.vicpoo.gasolineproject.core.platform.rememberFileSharer
import org.vicpoo.gasolineproject.core.ui.adaptive.LocalWindowWidth
import org.vicpoo.gasolineproject.core.ui.adaptive.WindowWidth
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.AppDropdownField
import org.vicpoo.gasolineproject.core.ui.components.DropdownOption
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OfflineBanner
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.ScreenHeader
import org.vicpoo.gasolineproject.core.ui.components.SecondaryButton
import org.vicpoo.gasolineproject.core.ui.components.StatusChip
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reports.domain.FleetReport
import org.vicpoo.gasolineproject.feature.reports.domain.ReportFormat
import org.vicpoo.gasolineproject.feature.reports.domain.ReportPeriod
import org.vicpoo.gasolineproject.feature.reports.domain.ReportSummary
import org.vicpoo.gasolineproject.feature.reports.domain.VehicleStats
import org.vicpoo.gasolineproject.feature.reports.domain.VehicleStatus
import org.vicpoo.gasolineproject.feature.reports.resources.Res
import org.vicpoo.gasolineproject.feature.reports.resources.reports_active_vehicles
import org.vicpoo.gasolineproject.feature.reports.resources.reports_download_error
import org.vicpoo.gasolineproject.feature.reports.resources.reports_download_excel
import org.vicpoo.gasolineproject.feature.reports.resources.reports_download_hint
import org.vicpoo.gasolineproject.feature.reports.resources.reports_download_pdf
import org.vicpoo.gasolineproject.feature.reports.resources.reports_empty
import org.vicpoo.gasolineproject.feature.reports.resources.reports_generated_at
import org.vicpoo.gasolineproject.feature.reports.resources.reports_offline
import org.vicpoo.gasolineproject.feature.reports.resources.reports_other_filters
import org.vicpoo.gasolineproject.feature.reports.resources.reports_period_all_time
import org.vicpoo.gasolineproject.feature.reports.resources.reports_period_last_3_months
import org.vicpoo.gasolineproject.feature.reports.resources.reports_period_last_month
import org.vicpoo.gasolineproject.feature.reports.resources.reports_period_this_month
import org.vicpoo.gasolineproject.feature.reports.resources.reports_period_this_year
import org.vicpoo.gasolineproject.feature.reports.resources.reports_range
import org.vicpoo.gasolineproject.feature.reports.resources.reports_range_all_time
import org.vicpoo.gasolineproject.feature.reports.resources.reports_stat_cost_per_km
import org.vicpoo.gasolineproject.feature.reports.resources.reports_stat_efficiency
import org.vicpoo.gasolineproject.feature.reports.resources.reports_stat_km
import org.vicpoo.gasolineproject.feature.reports.resources.reports_stat_liters
import org.vicpoo.gasolineproject.feature.reports.resources.reports_stat_loads
import org.vicpoo.gasolineproject.feature.reports.resources.reports_stat_price_per_liter
import org.vicpoo.gasolineproject.feature.reports.resources.reports_stat_spent
import org.vicpoo.gasolineproject.feature.reports.resources.reports_stat_trips
import org.vicpoo.gasolineproject.feature.reports.resources.reports_status_available
import org.vicpoo.gasolineproject.feature.reports.resources.reports_status_busy
import org.vicpoo.gasolineproject.feature.reports.resources.reports_status_maintenance
import org.vicpoo.gasolineproject.feature.reports.resources.reports_status_retired
import org.vicpoo.gasolineproject.feature.reports.resources.reports_summary_title
import org.vicpoo.gasolineproject.feature.reports.resources.reports_title
import org.vicpoo.gasolineproject.feature.reports.resources.reports_value_km
import org.vicpoo.gasolineproject.feature.reports.resources.reports_value_km_per_liter
import org.vicpoo.gasolineproject.feature.reports.resources.reports_value_liters
import org.vicpoo.gasolineproject.feature.reports.resources.reports_value_per_km
import org.vicpoo.gasolineproject.feature.reports.resources.reports_value_per_liter
import org.vicpoo.gasolineproject.feature.reports.resources.reports_vehicle_all
import org.vicpoo.gasolineproject.feature.reports.resources.reports_vehicle_details
import org.vicpoo.gasolineproject.feature.reports.resources.reports_vehicle_label
import org.vicpoo.gasolineproject.feature.reports.resources.reports_vehicle_last_trip
import org.vicpoo.gasolineproject.feature.reports.resources.reports_vehicle_no_trips
import org.vicpoo.gasolineproject.feature.reports.resources.reports_vehicles_title

/** Id del selector para "Toda la flota". */
private const val ALL_VEHICLES = "__all__"

@Composable
fun ReportsRoute(viewModel: ReportsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharer = rememberFileSharer()

    // Al volver a la pestaña (o a la app) se actualizan los números sin mostrar el indicador
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshSilently()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReportsEffect.Share -> sharer.share(SharedFile(effect.file.name, effect.file.mimeType, effect.file.bytes))
            }
        }
    }
    ReportsScreen(state = state, onEvent = viewModel::onEvent)
}

/**
 * Reportes de la flota: filtros (periodo y vehículo), resumen general, detalle por vehículo y
 * descarga en PDF o Excel (los genera la API; aquí solo se comparte el archivo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit) {
    val dimens = AppTheme.dimens
    val wide = LocalWindowWidth.current != WindowWidth.Compact
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
            ScreenHeader(
                title = stringResource(Res.string.reports_title),
                subtitle = state.report?.let { subtitleOf(it) },
            )
            Spacer(Modifier.height(dimens.spaceS))

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(ReportsEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = dimens.spaceL),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                ) {
                    item(key = "filters") { Filters(state, onEvent) }

                    if (state.isOffline || state.error != null || state.showingOtherFilters) {
                        item(key = "notices") { Notices(state) }
                    }

                    val report = state.report
                    when {
                        state.isInitialLoading -> item(key = "loading") { LoadingView(Modifier.fillMaxWidth().height(240.dp)) }
                        report == null -> item(key = "empty") { EmptyView(stringResource(Res.string.reports_empty)) }
                        else -> {
                            item(key = "summary") { SummarySection(report.summary, columns = if (wide) 4 else 2) }
                            item(key = "downloads") { Downloads(state, onEvent, sideBySide = wide) }
                            item(key = "vehicles_title") {
                                Text(
                                    stringResource(Res.string.reports_vehicles_title),
                                    style = AppTheme.typography.subtitle,
                                    color = AppTheme.colors.title,
                                )
                            }
                            if (report.vehicles.isEmpty()) {
                                item(key = "vehicles_empty") { EmptyView(stringResource(Res.string.reports_empty)) }
                            } else {
                                // Teléfono: una columna; tablet: dos tarjetas por fila
                                val perRow = if (wide) 2 else 1
                                val rows = report.vehicles.chunked(perRow)
                                rows.forEachIndexed { index, row ->
                                    item(key = "vehicle_row_${row.first().id}_$index") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceM)) {
                                            row.forEach { VehicleCard(it, Modifier.weight(1f)) }
                                            repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                            item(key = "generated") {
                                Text(
                                    stringResource(
                                        Res.string.reports_generated_at,
                                        AppDates.shortDate(report.generatedAt),
                                        AppDates.time12(report.generatedAt),
                                    ),
                                    style = AppTheme.typography.caption,
                                    color = AppTheme.colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun subtitleOf(report: FleetReport): String {
    val range = if (report.from == null || report.to == null) {
        stringResource(Res.string.reports_range_all_time)
    } else {
        stringResource(Res.string.reports_range, AppDates.shortDate(report.from), AppDates.shortDate(report.to))
    }
    return listOf(report.companyName, range).filter { it.isNotBlank() }.joinToString(" · ")
}

// ---------------- Filtros ----------------

@Composable
private fun Filters(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit) {
    val dimens = AppTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            ReportPeriod.entries.forEach { period ->
                PeriodChip(
                    text = stringResource(period.label()),
                    selected = state.filters.period == period,
                    onClick = { onEvent(ReportsEvent.PeriodSelected(period)) },
                )
            }
        }
        val allLabel = stringResource(Res.string.reports_vehicle_all)
        val options = listOf(DropdownOption(ALL_VEHICLES, allLabel)) +
            state.vehicles.map { DropdownOption(it.id, it.label) }
        AppDropdownField(
            options = options,
            selectedId = state.filters.vehicleId ?: ALL_VEHICLES,
            onSelect = { id -> onEvent(ReportsEvent.VehicleSelected(id.takeUnless { it == ALL_VEHICLES })) },
            placeholder = allLabel,
            label = stringResource(Res.string.reports_vehicle_label),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PeriodChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = AppTheme.typography.caption) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
            containerColor = colors.background,
            labelColor = colors.text,
        ),
    )
}

private fun ReportPeriod.label(): StringResource = when (this) {
    ReportPeriod.THIS_MONTH -> Res.string.reports_period_this_month
    ReportPeriod.LAST_MONTH -> Res.string.reports_period_last_month
    ReportPeriod.LAST_3_MONTHS -> Res.string.reports_period_last_3_months
    ReportPeriod.THIS_YEAR -> Res.string.reports_period_this_year
    ReportPeriod.ALL_TIME -> Res.string.reports_period_all_time
}

@Composable
private fun Notices(state: ReportsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.spaceS)) {
        if (state.isOffline) {
            OfflineBanner(stringResource(Res.string.reports_offline), lastUpdatedAtMillis = state.lastUpdatedAtMillis)
        }
        if (state.showingOtherFilters && state.report != null) {
            Text(
                stringResource(Res.string.reports_other_filters),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
        }
        state.error?.let { ErrorText(it.asText()) }
    }
}

// ---------------- Resumen ----------------

private class Stat(val label: StringResource, val value: String)

@Composable
private fun SummarySection(summary: ReportSummary, columns: Int) {
    val dimens = AppTheme.dimens
    val km = stringResource(Res.string.reports_value_km, ReportFormatting.number(summary.km))
    val liters = stringResource(Res.string.reports_value_liters, ReportFormatting.number(summary.liters))
    val efficiency = summary.kmPerLiter
        ?.let { stringResource(Res.string.reports_value_km_per_liter, ReportFormatting.number(it)) } ?: "—"
    val costPerKm = summary.costPerKm
        ?.let { stringResource(Res.string.reports_value_per_km, ReportFormatting.money(it)) } ?: "—"
    val pricePerLiter = summary.avgPricePerLiter
        ?.let { stringResource(Res.string.reports_value_per_liter, ReportFormatting.money(it)) } ?: "—"

    // Lo que pidió el negocio va primero: km, cargas y dinero
    val stats = listOf(
        Stat(Res.string.reports_stat_km, km),
        Stat(Res.string.reports_stat_loads, ReportFormatting.integer(summary.fuelLoads)),
        Stat(Res.string.reports_stat_spent, ReportFormatting.money(summary.spent)),
        Stat(Res.string.reports_stat_trips, ReportFormatting.integer(summary.trips)),
        Stat(Res.string.reports_stat_liters, liters),
        Stat(Res.string.reports_stat_efficiency, efficiency),
        Stat(Res.string.reports_stat_cost_per_km, costPerKm),
        Stat(Res.string.reports_stat_price_per_liter, pricePerLiter),
    )

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
        Text(
            stringResource(Res.string.reports_summary_title),
            style = AppTheme.typography.subtitle,
            color = AppTheme.colors.title,
        )
        Text(
            stringResource(Res.string.reports_active_vehicles, summary.activeVehicles, summary.vehicles),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textSecondary,
        )
        stats.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                row.forEach { StatCard(stringResource(it.label), it.value, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(AppTheme.dimens.spaceXs)) {
            Text(label, style = AppTheme.typography.caption, color = AppTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(AppTheme.dimens.spaceXs))
            Text(value, style = AppTheme.typography.subtitle, color = AppTheme.colors.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------- Descargas ----------------

@Composable
private fun Downloads(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit, sideBySide: Boolean) {
    val dimens = AppTheme.dimens
    val pdf: @Composable (Modifier) -> Unit = { modifier ->
        PrimaryButton(
            text = stringResource(Res.string.reports_download_pdf),
            onClick = { onEvent(ReportsEvent.Download(ReportFormat.PDF)) },
            modifier = modifier,
            enabled = state.canDownload,
            isLoading = state.downloading == ReportFormat.PDF,
            leadingIcon = AppIcons.Pdf,
        )
    }
    val excel: @Composable (Modifier) -> Unit = { modifier ->
        SecondaryButton(
            text = stringResource(Res.string.reports_download_excel),
            onClick = { onEvent(ReportsEvent.Download(ReportFormat.EXCEL)) },
            modifier = modifier,
            enabled = state.canDownload,
            isLoading = state.downloading == ReportFormat.EXCEL,
            leadingIcon = AppIcons.Xls,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceM)) {
                pdf(Modifier.weight(1f))
                excel(Modifier.weight(1f))
            }
        } else {
            pdf(Modifier.fillMaxWidth())
            excel(Modifier.fillMaxWidth())
        }
        Text(
            stringResource(Res.string.reports_download_hint),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textSecondary,
        )
        state.downloadError?.let { ErrorText(stringResource(Res.string.reports_download_error, it.asText())) }
    }
}

// ---------------- Por vehículo ----------------

@Composable
private fun VehicleCard(vehicle: VehicleStats, modifier: Modifier = Modifier) {
    val dimens = AppTheme.dimens
    val colors = AppTheme.colors
    AppCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(dimens.spaceXs), verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(vehicle.plates, style = AppTheme.typography.subtitle, color = colors.title)
                    val name = listOfNotNull(vehicle.brand, vehicle.model, vehicle.year?.toString())
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                    if (name.isNotBlank()) {
                        Text(name, style = AppTheme.typography.bodySecondary, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                VehicleStatusChip(vehicle)
            }

            // Lo principal: km, cargas y dinero
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat(stringResource(Res.string.reports_stat_km), ReportFormatting.number(vehicle.km))
                MiniStat(stringResource(Res.string.reports_stat_loads), ReportFormatting.integer(vehicle.fuelLoads))
                MiniStat(stringResource(Res.string.reports_stat_spent), ReportFormatting.money(vehicle.spent))
            }

            Text(
                stringResource(
                    Res.string.reports_vehicle_details,
                    ReportFormatting.integer(vehicle.trips),
                    stringResource(Res.string.reports_value_liters, ReportFormatting.number(vehicle.liters)),
                    vehicle.kmPerLiter?.let { stringResource(Res.string.reports_value_km_per_liter, ReportFormatting.number(it)) } ?: "— km/L",
                    vehicle.costPerKm?.let { stringResource(Res.string.reports_value_per_km, ReportFormatting.money(it)) } ?: "— /km",
                ),
                style = AppTheme.typography.caption,
                color = colors.textSecondary,
            )
            Text(
                vehicle.lastTripAt
                    ?.let { stringResource(Res.string.reports_vehicle_last_trip, AppDates.shortDate(it)) }
                    ?: stringResource(Res.string.reports_vehicle_no_trips),
                style = AppTheme.typography.caption,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(label, style = AppTheme.typography.caption, color = AppTheme.colors.textSecondary)
        Text(value, style = AppTheme.typography.body, color = AppTheme.colors.title)
    }
}

@Composable
private fun VehicleStatusChip(vehicle: VehicleStats) {
    val colors = AppTheme.colors
    val (text, container, content) = when {
        // Mismos colores que los estados en la pestaña de vehículos (VehicleUi)
        vehicle.retired -> Triple(Res.string.reports_status_retired, colors.labelBackground, colors.onLabel)
        vehicle.status == VehicleStatus.AVAILABLE -> Triple(Res.string.reports_status_available, colors.primary, colors.onPrimary)
        vehicle.status == VehicleStatus.BUSY -> Triple(Res.string.reports_status_busy, colors.secondary, colors.onSecondary)
        vehicle.status == VehicleStatus.MAINTENANCE -> Triple(Res.string.reports_status_maintenance, colors.danger, colors.onDanger)
        else -> return
    }
    StatusChip(text = stringResource(text), containerColor = container, contentColor = content)
}
