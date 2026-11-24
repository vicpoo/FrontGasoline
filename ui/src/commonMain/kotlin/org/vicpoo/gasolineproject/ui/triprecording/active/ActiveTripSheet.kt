package org.vicpoo.gasolineproject.ui.triprecording.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecord
import org.vicpoo.gasolineproject.domain.triprecording.RecordSyncState
import org.vicpoo.gasolineproject.ui.common.components.PendingSyncChip
import org.vicpoo.gasolineproject.ui.common.components.PrimaryButton
import org.vicpoo.gasolineproject.ui.common.components.SecondaryText
import org.vicpoo.gasolineproject.ui.common.components.SyncErrorChip
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.active_trip_add_fuel
import org.vicpoo.gasolineproject.ui.resources.active_trip_date
import org.vicpoo.gasolineproject.ui.resources.active_trip_distance
import org.vicpoo.gasolineproject.ui.resources.active_trip_duration
import org.vicpoo.gasolineproject.ui.resources.active_trip_finish
import org.vicpoo.gasolineproject.ui.resources.active_trip_fuel_detail
import org.vicpoo.gasolineproject.ui.resources.active_trip_fuel_item
import org.vicpoo.gasolineproject.ui.resources.active_trip_no_fuel
import org.vicpoo.gasolineproject.ui.resources.active_trip_pause
import org.vicpoo.gasolineproject.ui.resources.active_trip_resume
import org.vicpoo.gasolineproject.ui.resources.cd_delete_fuel
import org.vicpoo.gasolineproject.ui.resources.cd_edit_fuel
import org.vicpoo.gasolineproject.ui.resources.cd_finish_trip
import org.vicpoo.gasolineproject.ui.resources.cd_pause_trip
import org.vicpoo.gasolineproject.ui.resources.cd_resume_trip
import org.vicpoo.gasolineproject.ui.triprecording.common.TripFormat

/* Hoja inferior del viaje en curso: datos, pausa / reanudar, terminar y recargas (punto 3.2.3). */

/**
 * Alto de la hoja cuando se desliza hacia abajo: agarradera + duración, distancia y fecha.
 * (Se le suma la barra de navegación del sistema.)
 */
internal val SHEET_PEEK_HEIGHT = 100.dp

/**
 * Contenido de la hoja inferior: estadísticas (siempre visibles), pausa / finalizar,
 * "registrar recarga" y lista de recargas. La agarradera la pone BottomSheetScaffold.
 */
@Composable
internal fun TripSheet(
    state: ActiveTripUiState,
    onEvent: (ActiveTripEvent) -> Unit,
    onAddFuel: () -> Unit,
    onEditFuel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = AppTheme.dimens
    val trip = state.session.trip ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = dimens.screenPadding, end = dimens.screenPadding, bottom = dimens.spaceM),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(stringResource(Res.string.active_trip_duration), TripFormat.duration(state.elapsedMillis))
            Stat(stringResource(Res.string.active_trip_distance), TripFormat.km(trip.distanceMeters))
            Stat(stringResource(Res.string.active_trip_date), TripFormat.dayMonth(trip.startedAt))
        }
        Spacer(Modifier.height(dimens.spaceM))

        // Pausa / reanudar a la izquierda y el botón grande ■ de terminar (Figma)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PauseButton(
                    isPaused = state.isPaused,
                    isLoading = state.isChangingPause,
                    onClick = {
                        onEvent(if (state.isPaused) ActiveTripEvent.ResumeClicked else ActiveTripEvent.PauseClicked)
                    },
                )
                Spacer(Modifier.height(dimens.spaceXs))
                SecondaryText(
                    stringResource(if (state.isPaused) Res.string.active_trip_resume else Res.string.active_trip_pause),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FinishButton(isLoading = state.isStopping, onClick = { onEvent(ActiveTripEvent.FinishClicked) })
                Spacer(Modifier.height(dimens.spaceXs))
                SecondaryText(stringResource(Res.string.active_trip_finish))
            }
        }
        Spacer(Modifier.height(dimens.spaceM))

        if (state.fuel.canAdd) {
            PrimaryButton(
                text = stringResource(Res.string.active_trip_add_fuel),
                onClick = onAddFuel,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = AppIcons.Fuel,
            )
            Spacer(Modifier.height(dimens.spaceM))
        }

        if (state.fuelRecords.isEmpty()) {
            SecondaryText(stringResource(Res.string.active_trip_no_fuel), textAlign = TextAlign.Center)
        } else {
            state.fuelRecords.forEach { record ->
                FuelRecordRow(
                    record = record,
                    canEdit = state.fuel.canEdit,
                    canDelete = state.fuel.canDelete,
                    onEdit = { onEditFuel(record.localId) },
                    onDelete = { onEvent(ActiveTripEvent.DeleteClicked(record)) },
                )
                Spacer(Modifier.height(dimens.spaceS))
            }
        }
    }
}

@Composable
internal fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = AppTheme.typography.caption, color = AppTheme.colors.textSecondary)
        Text(
            value,
            style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.Medium),
            color = AppTheme.colors.title,
        )
    }
}

/** Botón redondo para pausar (‖) o reanudar (▷) el viaje. */
@Composable
internal fun PauseButton(isPaused: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = CircleShape,
        color = colors.secondary,
        shadowElevation = 4.dp,
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(color = colors.onSecondary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    if (isPaused) AppIcons.Play else AppIcons.Pause,
                    contentDescription = stringResource(if (isPaused) Res.string.cd_resume_trip else Res.string.cd_pause_trip),
                    tint = colors.onSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

/** Botón redondo grande (Figma) para terminar el viaje. */
@Composable
internal fun FinishButton(isLoading: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = CircleShape,
        color = colors.primary,
        shadowElevation = 4.dp,
        modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            } else {
                Icon(
                    AppIcons.Stop,
                    contentDescription = stringResource(Res.string.cd_finish_trip),
                    tint = colors.onPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/** "Recarga 1#" con editar y eliminar. */
@Composable
internal fun FuelRecordRow(
    record: FuelRecord,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radius),
        color = colors.labelBackground,
    ) {
        Column(Modifier.padding(start = dimens.spaceM, end = dimens.spaceXs, top = dimens.spaceXs, bottom = dimens.spaceS)) {
            // Fila: ícono · "Recarga 1#" y "$2,000 • 25 L" · editar · eliminar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Fuel, contentDescription = null, tint = colors.onLabel, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(dimens.spaceS))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.active_trip_fuel_item, record.number),
                        style = AppTheme.typography.body,
                        color = colors.onLabel,
                        maxLines = 1,
                    )
                    Text(
                        stringResource(
                            Res.string.active_trip_fuel_detail,
                            TripFormat.money(record.amount),
                            TripFormat.number(record.liters),
                        ),
                        style = AppTheme.typography.caption,
                        color = colors.onLabel,
                        maxLines = 1,
                    )
                }
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(AppIcons.Pencil, contentDescription = stringResource(Res.string.cd_edit_fuel), tint = colors.onLabel)
                    }
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(AppIcons.Trash, contentDescription = stringResource(Res.string.cd_delete_fuel), tint = colors.danger)
                    }
                }
            }
            // Estado de envío y el motivo del error, debajo y a todo lo ancho (así no aprietan el texto)
            if (record.syncState != RecordSyncState.SYNCED) {
                Spacer(Modifier.height(dimens.spaceXs))
                when (record.syncState) {
                    RecordSyncState.PENDING -> PendingSyncChip()
                    RecordSyncState.ERROR -> SyncErrorChip()
                    RecordSyncState.SYNCED -> Unit
                }
                record.syncErrorMessage?.let {
                    Spacer(Modifier.height(dimens.spaceXs))
                    Text(it, style = AppTheme.typography.caption, color = colors.error)
                }
            }
        }
    }
}
