package org.vicpoo.gasolineproject.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.domain.reports.ReportFormat
import org.vicpoo.gasolineproject.domain.reports.ReportSummary
import org.vicpoo.gasolineproject.ui.common.components.AppCard
import org.vicpoo.gasolineproject.ui.common.components.ErrorText
import org.vicpoo.gasolineproject.ui.common.components.OfflineBanner
import org.vicpoo.gasolineproject.ui.common.components.PrimaryButton
import org.vicpoo.gasolineproject.ui.common.components.SecondaryButton
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.reports_active_vehicles
import org.vicpoo.gasolineproject.ui.resources.reports_download_error
import org.vicpoo.gasolineproject.ui.resources.reports_download_excel
import org.vicpoo.gasolineproject.ui.resources.reports_download_pdf
import org.vicpoo.gasolineproject.ui.resources.reports_offline
import org.vicpoo.gasolineproject.ui.resources.reports_other_filters
import org.vicpoo.gasolineproject.ui.resources.reports_stat_cost_per_km
import org.vicpoo.gasolineproject.ui.resources.reports_stat_efficiency
import org.vicpoo.gasolineproject.ui.resources.reports_stat_km
import org.vicpoo.gasolineproject.ui.resources.reports_stat_liters
import org.vicpoo.gasolineproject.ui.resources.reports_stat_loads
import org.vicpoo.gasolineproject.ui.resources.reports_stat_price_per_liter
import org.vicpoo.gasolineproject.ui.resources.reports_stat_spent
import org.vicpoo.gasolineproject.ui.resources.reports_stat_trips
import org.vicpoo.gasolineproject.ui.resources.reports_summary_title
import org.vicpoo.gasolineproject.ui.resources.reports_value_km
import org.vicpoo.gasolineproject.ui.resources.reports_value_km_per_liter
import org.vicpoo.gasolineproject.ui.resources.reports_value_liters
import org.vicpoo.gasolineproject.ui.resources.reports_value_per_km
import org.vicpoo.gasolineproject.ui.resources.reports_value_per_liter

/* Secciones de Reportes: avisos, resumen y descargas (punto 3.2.3). */

@Composable
internal fun Notices(state: ReportsUiState) {
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

internal class Stat(val label: StringResource, val value: String)

@Composable
internal fun SummarySection(summary: ReportSummary, columns: Int) {
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
internal fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(AppTheme.dimens.spaceXs)) {
            Text(label, style = AppTheme.typography.caption, color = AppTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(AppTheme.dimens.spaceXs))
            Text(value, style = AppTheme.typography.subtitle, color = AppTheme.colors.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun Downloads(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit, sideBySide: Boolean) {
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
        state.downloadError?.let { ErrorText(stringResource(Res.string.reports_download_error, it.asText())) }
    }
}
