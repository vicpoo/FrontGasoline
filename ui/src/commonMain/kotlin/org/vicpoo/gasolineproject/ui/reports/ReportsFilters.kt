package org.vicpoo.gasolineproject.ui.reports

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.domain.common.AppDates
import org.vicpoo.gasolineproject.domain.reports.ReportPeriod
import org.vicpoo.gasolineproject.domain.reports.ReportPeriods
import org.vicpoo.gasolineproject.ui.common.components.AppDropdownField
import org.vicpoo.gasolineproject.ui.common.components.DropdownOption
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.reports_custom_range
import org.vicpoo.gasolineproject.ui.resources.reports_date_picker_apply
import org.vicpoo.gasolineproject.ui.resources.reports_date_picker_cancel
import org.vicpoo.gasolineproject.ui.resources.reports_date_picker_end
import org.vicpoo.gasolineproject.ui.resources.reports_date_picker_start
import org.vicpoo.gasolineproject.ui.resources.reports_date_picker_title
import org.vicpoo.gasolineproject.ui.resources.reports_period_all_time
import org.vicpoo.gasolineproject.ui.resources.reports_period_custom
import org.vicpoo.gasolineproject.ui.resources.reports_period_last_3_months
import org.vicpoo.gasolineproject.ui.resources.reports_period_last_month
import org.vicpoo.gasolineproject.ui.resources.reports_period_this_month
import org.vicpoo.gasolineproject.ui.resources.reports_period_this_year
import org.vicpoo.gasolineproject.ui.resources.reports_vehicle_all
import org.vicpoo.gasolineproject.ui.resources.reports_vehicle_label

/* Filtros de Reportes: vehículo, periodo y rango de fechas (separados de ReportsScreen, punto 3.2.3). */

@Composable
internal fun Filters(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit) {
    val dimens = AppTheme.dimens
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val custom = state.filters.period == ReportPeriod.CUSTOM
    val from = state.filters.customFrom
    val to = state.filters.customTo
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
        ) {
            // Primero: mismo tamaño que "Este mes", con calendario (el usuario elige de qué fecha a qué fecha)
            PeriodChip(
                text = if (custom && from != null) {
                    stringResource(
                        Res.string.reports_custom_range,
                        AppDates.shortDate(from),
                        AppDates.shortDate(to ?: from),
                    )
                } else {
                    stringResource(Res.string.reports_period_custom)
                },
                selected = custom,
                onClick = { showDatePicker = true },
                leadingIcon = AppIcons.Calendar,
            )
            ReportPeriod.entries.filter { it != ReportPeriod.CUSTOM }.forEach { period ->
                PeriodChip(
                    text = stringResource(period.label()),
                    selected = state.filters.period == period,
                    onClick = { onEvent(ReportsEvent.PeriodSelected(period)) },
                )
            }
        }
        // El filtro de vehículo va abajo del filtro de fechas
        VehicleFilter(state, onEvent)
    }
    if (showDatePicker) {
        ReportDateRangeDialog(
            initialFrom = from,
            initialTo = to,
            onDismiss = { showDatePicker = false },
            onApply = { start, end ->
                showDatePicker = false
                onEvent(ReportsEvent.CustomRangeSelected(start, end))
            },
        )
    }
}

/** Filtro de vehículo (abajo del filtro de fechas). */
@Composable
internal fun VehicleFilter(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit) {
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

@Composable
internal fun PeriodChip(text: String, selected: Boolean, onClick: () -> Unit, leadingIcon: ImageVector? = null) {
    val colors = AppTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = AppTheme.typography.caption) },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
            selectedLeadingIconColor = colors.onPrimary,
            containerColor = colors.background,
            labelColor = colors.text,
            iconColor = colors.text,
        ),
    )
}

/**
 * Calendario para elegir "de qué fecha a qué fecha". No deja elegir días futuros.
 * El DateRangePicker trabaja en milisegundos UTC a medianoche; se convierten a "yyyy-MM-dd".
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
internal fun ReportDateRangeDialog(
    initialFrom: String?,
    initialTo: String?,
    onDismiss: () -> Unit,
    onApply: (from: String, to: String) -> Unit,
) {
    val colors = AppTheme.colors
    // Hasta hoy (con margen por la zona horaria: el calendario usa UTC)
    val lastSelectable = remember { Clock.System.now().toEpochMilliseconds() + 14 * 60 * 60 * 1000L }
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = ReportPeriods.utcMillisOf(initialFrom),
        initialSelectedEndDateMillis = ReportPeriods.utcMillisOf(initialTo),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= lastSelectable
        },
    )
    val pickerColors = DatePickerDefaults.colors(
        containerColor = colors.background,
        titleContentColor = colors.title,
        headlineContentColor = colors.title,
        weekdayContentColor = colors.textSecondary,
        subheadContentColor = colors.text,
        navigationContentColor = colors.text,
        yearContentColor = colors.text,
        currentYearContentColor = colors.primary,
        selectedYearContentColor = colors.onPrimary,
        selectedYearContainerColor = colors.primary,
        dayContentColor = colors.text,
        selectedDayContentColor = colors.onPrimary,
        selectedDayContainerColor = colors.primary,
        todayContentColor = colors.primary,
        todayDateBorderColor = colors.primary,
        dayInSelectionRangeContentColor = colors.text,
        dayInSelectionRangeContainerColor = colors.primary.copy(alpha = if (colors.isDark) 0.35f else 0.18f),
    )
    val start = pickerState.selectedStartDateMillis
    val end = pickerState.selectedEndDateMillis
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = pickerColors,
        confirmButton = {
            TextButton(
                enabled = start != null,
                onClick = {
                    val from = ReportPeriods.dateFromUtcMillis(start ?: return@TextButton)
                    // Si solo eligió un día, el reporte es de ese día
                    val to = ReportPeriods.dateFromUtcMillis(end ?: start)
                    onApply(from, to)
                },
            ) { Text(stringResource(Res.string.reports_date_picker_apply), color = colors.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.reports_date_picker_cancel), color = colors.text)
            }
        },
    ) {
        DateRangePicker(
            state = pickerState,
            modifier = Modifier.weight(1f),
            colors = pickerColors,
            // Título y fechas elegidas con letra chica (el encabezado por defecto es enorme)
            title = {
                Text(
                    stringResource(Res.string.reports_date_picker_title),
                    style = AppTheme.typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            headline = {
                val startText = pickerState.selectedStartDateMillis
                    ?.let { AppDates.shortDate(ReportPeriods.dateFromUtcMillis(it)) }
                    ?: stringResource(Res.string.reports_date_picker_start)
                val endText = pickerState.selectedEndDateMillis
                    ?.let { AppDates.shortDate(ReportPeriods.dateFromUtcMillis(it)) }
                    ?: stringResource(Res.string.reports_date_picker_end)
                Text(
                    "$startText – $endText",
                    style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.title,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                )
            },
        )
    }
}

internal fun ReportPeriod.label(): StringResource = when (this) {
    ReportPeriod.THIS_MONTH -> Res.string.reports_period_this_month
    ReportPeriod.LAST_MONTH -> Res.string.reports_period_last_month
    ReportPeriod.LAST_3_MONTHS -> Res.string.reports_period_last_3_months
    ReportPeriod.THIS_YEAR -> Res.string.reports_period_this_year
    ReportPeriod.ALL_TIME -> Res.string.reports_period_all_time
    ReportPeriod.CUSTOM -> Res.string.reports_period_custom
}
