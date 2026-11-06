package org.vicpoo.gasolineproject.ui.triprecording.syncstatus

import org.vicpoo.gasolineproject.domain.sync.PendingSummaryItem
import org.vicpoo.gasolineproject.domain.sync.PendingSyncKind
import org.vicpoo.gasolineproject.ui.resources.sync_status_others_title
import org.vicpoo.gasolineproject.ui.resources.sync_status_others_hint
import org.vicpoo.gasolineproject.ui.resources.sync_status_others_summary
import org.vicpoo.gasolineproject.ui.resources.sync_status_kind_reservations
import org.vicpoo.gasolineproject.ui.resources.sync_status_kind_vehicles
import org.vicpoo.gasolineproject.ui.resources.sync_status_kind_other
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.domain.common.AppDates
import org.vicpoo.gasolineproject.ui.common.components.AppBackButton
import org.vicpoo.gasolineproject.ui.common.components.AppCard
import org.vicpoo.gasolineproject.ui.common.components.AppTextButton
import org.vicpoo.gasolineproject.ui.common.components.BodyText
import org.vicpoo.gasolineproject.ui.common.components.ErrorText
import org.vicpoo.gasolineproject.ui.common.components.LoadingView
import org.vicpoo.gasolineproject.ui.common.components.PrimaryButton
import org.vicpoo.gasolineproject.ui.common.components.ResultDialog
import org.vicpoo.gasolineproject.ui.common.components.ResultType
import org.vicpoo.gasolineproject.ui.common.components.SecondaryText
import org.vicpoo.gasolineproject.ui.common.components.StatusChip
import org.vicpoo.gasolineproject.ui.common.components.SubtitleText
import org.vicpoo.gasolineproject.ui.common.components.TitleText
import org.vicpoo.gasolineproject.ui.common.components.AppScreen
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.domain.triprecording.PendingFuelStatus
import org.vicpoo.gasolineproject.domain.triprecording.PendingTripState
import org.vicpoo.gasolineproject.domain.triprecording.PendingTripStatus
import org.vicpoo.gasolineproject.domain.triprecording.RecordSyncState
import org.vicpoo.gasolineproject.domain.triprecording.SyncLogEntry
import org.vicpoo.gasolineproject.domain.triprecording.SyncLogResult
import org.vicpoo.gasolineproject.ui.triprecording.common.TripFormat
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.action_understood
import org.vicpoo.gasolineproject.ui.resources.sync_status_all_sent
import org.vicpoo.gasolineproject.ui.resources.sync_status_fuel_error
import org.vicpoo.gasolineproject.ui.resources.sync_status_fuel_line
import org.vicpoo.gasolineproject.ui.resources.sync_status_fuel_pending
import org.vicpoo.gasolineproject.ui.resources.sync_status_last_success
import org.vicpoo.gasolineproject.ui.resources.sync_status_log_empty
import org.vicpoo.gasolineproject.ui.resources.sync_status_log_less
import org.vicpoo.gasolineproject.ui.resources.sync_status_log_more
import org.vicpoo.gasolineproject.ui.resources.sync_status_log_title
import org.vicpoo.gasolineproject.ui.resources.sync_status_never_sent
import org.vicpoo.gasolineproject.ui.resources.sync_status_odometer_pending
import org.vicpoo.gasolineproject.ui.resources.sync_status_pauses_pending
import org.vicpoo.gasolineproject.ui.resources.sync_status_pending_summary
import org.vicpoo.gasolineproject.ui.resources.sync_status_points_pending
import org.vicpoo.gasolineproject.ui.resources.sync_status_result_done_message
import org.vicpoo.gasolineproject.ui.resources.sync_status_result_done_title
import org.vicpoo.gasolineproject.ui.resources.sync_status_result_error_message
import org.vicpoo.gasolineproject.ui.resources.sync_status_result_error_title
import org.vicpoo.gasolineproject.ui.resources.sync_status_result_offline_message
import org.vicpoo.gasolineproject.ui.resources.sync_status_result_offline_title
import org.vicpoo.gasolineproject.ui.resources.sync_status_result_sending_message
import org.vicpoo.gasolineproject.ui.resources.sync_status_result_sending_title
import org.vicpoo.gasolineproject.ui.resources.sync_status_send_now
import org.vicpoo.gasolineproject.ui.resources.sync_status_sending
import org.vicpoo.gasolineproject.ui.resources.sync_status_server_no
import org.vicpoo.gasolineproject.ui.resources.sync_status_server_yes
import org.vicpoo.gasolineproject.ui.resources.sync_status_state_attention
import org.vicpoo.gasolineproject.ui.resources.sync_status_state_in_progress
import org.vicpoo.gasolineproject.ui.resources.sync_status_state_waiting
import org.vicpoo.gasolineproject.ui.resources.sync_status_title
import org.vicpoo.gasolineproject.ui.resources.sync_status_trip_started
import org.vicpoo.gasolineproject.ui.resources.sync_status_trips_title

@Composable
fun SyncStatusRoute(
    onBack: () -> Unit,
    viewModel: SyncStatusViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SyncStatusScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/** "Envíos pendientes": lo que sigue en el teléfono, por qué, y los últimos intentos de envío. */
@Composable
fun SyncStatusScreen(
    state: SyncStatusUiState,
    onEvent: (SyncStatusEvent) -> Unit,
    onBack: () -> Unit,
) {
    val dimens = AppTheme.dimens
    AppScreen {
        Spacer(Modifier.height(dimens.spaceM))
        AppBackButton(onClick = onBack)
        Spacer(Modifier.height(dimens.spaceM))
        TitleText(stringResource(Res.string.sync_status_title))
        Spacer(Modifier.height(dimens.spaceM))

        if (state.isLoading) {
            LoadingView()
            return@AppScreen
        }

        SummaryCard(state)
        Spacer(Modifier.height(dimens.spaceM))
        PrimaryButton(
            text = stringResource(if (state.isSending) Res.string.sync_status_sending else Res.string.sync_status_send_now),
            onClick = { onEvent(SyncStatusEvent.SendNowClicked) },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isSending,
            enabled = !state.isSending,
            leadingIcon = AppIcons.Upload,
        )

        if (state.status.trips.isNotEmpty()) {
            Spacer(Modifier.height(dimens.spaceL))
            SubtitleText(stringResource(Res.string.sync_status_trips_title))
            Spacer(Modifier.height(dimens.spaceS))
            state.status.trips.forEach { trip ->
                PendingTripCard(trip)
                Spacer(Modifier.height(dimens.spaceS))
            }
        }

        if (state.others.isNotEmpty()) {
            Spacer(Modifier.height(dimens.spaceL))
            SubtitleText(stringResource(Res.string.sync_status_others_title))
            Spacer(Modifier.height(dimens.spaceS))
            OthersCard(state.others)
        }

        Spacer(Modifier.height(dimens.spaceL))
        SubtitleText(stringResource(Res.string.sync_status_log_title))
        Spacer(Modifier.height(dimens.spaceS))
        val log = state.status.log
        if (log.isEmpty()) {
            SecondaryText(stringResource(Res.string.sync_status_log_empty))
        } else {
            val visible = if (state.showFullLog) log else log.take(COLLAPSED_LOG_ENTRIES)
            visible.forEach { entry ->
                LogEntryRow(entry)
                Spacer(Modifier.height(dimens.spaceS))
            }
            if (log.size > COLLAPSED_LOG_ENTRIES) {
                AppTextButton(
                    text = stringResource(if (state.showFullLog) Res.string.sync_status_log_less else Res.string.sync_status_log_more),
                    onClick = { onEvent(SyncStatusEvent.ToggleFullLog) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
        Spacer(Modifier.height(dimens.spaceXl))
    }

    SendResultDialog(result = state.sendResult, onDismiss = { onEvent(SyncStatusEvent.ResultDismissed) })
}

/** Resumen: todo enviado / cuántos pendientes, y el último envío correcto. */
@Composable
private fun SummaryCard(state: SyncStatusUiState) {
    val dimens = AppTheme.dimens
    val status = state.status
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(dimens.spaceM)) {
            if (state.isAllSent) {
                BodyText(stringResource(Res.string.sync_status_all_sent))
            } else {
                BodyText(stringResource(Res.string.sync_status_pending_summary, status.pendingFuelCount, status.trips.size))
                if (state.others.isNotEmpty()) {
                    BodyText(stringResource(Res.string.sync_status_others_summary, state.others.sumOf { it.count }))
                }
            }
            Spacer(Modifier.height(dimens.spaceXs))
            val lastSuccess = status.lastSuccess
            SecondaryText(
                if (lastSuccess != null) {
                    stringResource(Res.string.sync_status_last_success, dateTime(lastSuccess.at))
                } else {
                    stringResource(Res.string.sync_status_never_sent)
                },
            )
        }
    }
}

/** Lo pendiente del resto de la app, por grupo (ej. "Vehículos: 2"). */
@Composable
private fun OthersCard(items: List<PendingSummaryItem>) {
    val dimens = AppTheme.dimens
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(dimens.spaceM)) {
            items.forEach { item ->
                BodyText(
                    stringResource(
                        when (item.kind) {
                            PendingSyncKind.RESERVATIONS -> Res.string.sync_status_kind_reservations
                            PendingSyncKind.VEHICLES -> Res.string.sync_status_kind_vehicles
                            else -> Res.string.sync_status_kind_other
                        },
                        item.count,
                    ),
                )
            }
            Spacer(Modifier.height(dimens.spaceXs))
            SecondaryText(stringResource(Res.string.sync_status_others_hint))
        }
    }
}

@Composable
private fun PendingTripCard(trip: PendingTripStatus) {
    val dimens = AppTheme.dimens
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(dimens.spaceM)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    BodyText("${trip.vehicleTitle} · ${trip.plates}")
                    SecondaryText(stringResource(Res.string.sync_status_trip_started, dateTime(trip.startedAt)))
                }
                Spacer(Modifier.width(dimens.spaceS))
                TripStateChip(trip.state)
            }
            Spacer(Modifier.height(dimens.spaceS))
            SecondaryText(
                stringResource(if (trip.registeredOnServer) Res.string.sync_status_server_yes else Res.string.sync_status_server_no),
            )
            if (trip.pointsPending > 0) SecondaryText(stringResource(Res.string.sync_status_points_pending, trip.pointsPending))
            if (trip.pausesPending > 0) SecondaryText(stringResource(Res.string.sync_status_pauses_pending, trip.pausesPending))
            if (trip.odometerPhotoPending) SecondaryText(stringResource(Res.string.sync_status_odometer_pending))
            trip.errorMessage?.let {
                Spacer(Modifier.height(dimens.spaceXs))
                ErrorText(it)
            }
            trip.fuel.forEach { fuel ->
                Spacer(Modifier.height(dimens.spaceXs))
                FuelLine(fuel)
            }
        }
    }
}

@Composable
private fun FuelLine(fuel: PendingFuelStatus) {
    val line = stringResource(Res.string.sync_status_fuel_line, fuel.number, TripFormat.number(fuel.liters))
    when (fuel.state) {
        RecordSyncState.ERROR -> ErrorText(stringResource(Res.string.sync_status_fuel_error, line, fuel.errorMessage.orEmpty()))
        else -> BodyText(stringResource(Res.string.sync_status_fuel_pending, line))
    }
}

@Composable
private fun TripStateChip(state: PendingTripState) {
    val colors = AppTheme.colors
    when (state) {
        PendingTripState.IN_PROGRESS -> StatusChip(
            text = stringResource(Res.string.sync_status_state_in_progress),
            containerColor = colors.secondary,
            contentColor = colors.onSecondary,
        )
        PendingTripState.WAITING_TO_SEND -> StatusChip(
            text = stringResource(Res.string.sync_status_state_waiting),
            containerColor = colors.labelBackground,
            contentColor = colors.onLabel,
        )
        PendingTripState.NEEDS_ATTENTION -> StatusChip(
            text = stringResource(Res.string.sync_status_state_attention),
            containerColor = colors.danger,
            contentColor = colors.onDanger,
        )
    }
}

/** Un paso de la bitácora: hora, qué se intentó, resultado y cuánto tardó. */
@Composable
private fun LogEntryRow(entry: SyncLogEntry) {
    val colors = AppTheme.colors
    val (mark, color) = when (entry.result) {
        SyncLogResult.OK -> "✓" to colors.confirm
        SyncLogResult.OFFLINE -> "…" to colors.textSecondary
        SyncLogResult.REJECTED -> "✕" to colors.error
        SyncLogResult.ERROR -> "!" to colors.error
    }
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.spaceS)) {
        Text(mark, style = AppTheme.typography.body, color = color)
        Column(Modifier.weight(1f)) {
            val seconds = if (entry.durationMs > 0) " · ${formatSeconds(entry.durationMs)}" else ""
            Text("${dateTime(entry.at)} · ${entry.step}$seconds", style = AppTheme.typography.caption, color = colors.text)
            entry.message?.let { message ->
                val code = entry.code?.let { " ($it)" }.orEmpty()
                Text("$message$code", style = AppTheme.typography.caption, color = if (entry.result == SyncLogResult.OK) colors.textSecondary else color)
            }
        }
    }
}

@Composable
private fun SendResultDialog(result: SendNowResult?, onDismiss: () -> Unit) {
    val (type, title, message) = when (result ?: return) {
        SendNowResult.DONE -> Triple(ResultType.Success, Res.string.sync_status_result_done_title, Res.string.sync_status_result_done_message)
        SendNowResult.STILL_SENDING -> Triple(ResultType.Success, Res.string.sync_status_result_sending_title, Res.string.sync_status_result_sending_message)
        SendNowResult.OFFLINE -> Triple(ResultType.Error, Res.string.sync_status_result_offline_title, Res.string.sync_status_result_offline_message)
        SendNowResult.ERROR -> Triple(ResultType.Error, Res.string.sync_status_result_error_title, Res.string.sync_status_result_error_message)
    }
    ResultDialog(
        type = type,
        title = stringResource(title),
        message = stringResource(message),
        buttonText = stringResource(Res.string.action_understood),
        onButtonClick = onDismiss,
        onDismiss = onDismiss,
    )
}

/** "06/11/2026 3:45 pm" */
private fun dateTime(iso: String): String = "${AppDates.shortDate(iso)} ${AppDates.time12(iso)}".trim()

private fun formatSeconds(millis: Long): String {
    val tenths = (millis + 50) / 100
    return "${tenths / 10}.${tenths % 10} s"
}

private const val COLLAPSED_LOG_ENTRIES = 20
