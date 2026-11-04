package org.vicpoo.gasolineproject.feature.reservations.presentation.assignmentdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.AppButton
import org.vicpoo.gasolineproject.core.ui.components.AppButtonVariant
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorView
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OfflineBanner
import org.vicpoo.gasolineproject.core.ui.components.ResultDialog
import org.vicpoo.gasolineproject.core.ui.components.ResultType
import org.vicpoo.gasolineproject.core.ui.components.UserAvatar
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.domain.AssignmentEmployee
import org.vicpoo.gasolineproject.feature.reservations.domain.EmployeeVehicle
import org.vicpoo.gasolineproject.feature.reservations.presentation.assignments.ActiveTripChip
import org.vicpoo.gasolineproject.feature.reservations.presentation.assignments.IconBadge
import org.vicpoo.gasolineproject.feature.reservations.presentation.assignments.VehicleStateChip
import org.vicpoo.gasolineproject.feature.reservations.presentation.assignments.icon
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.formatDate
import org.vicpoo.gasolineproject.feature.reservations.resources.Res
import org.vicpoo.gasolineproject.feature.reservations.resources.action_cancel
import org.vicpoo.gasolineproject.feature.reservations.resources.action_understood
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_detail_holder
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_detail_not_found
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_detail_title
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_detail_vehicle
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_offline
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_plates_value
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_since
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_unlink
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_unlink_blocked
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_unlink_error_title
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_unlink_info
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_unlink_question
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_unlink_reason
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_unlink_reason_hint
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_unlinked_message
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_unlinked_title
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_vehicle_count

/** [key] = AssignmentKey.value ("r:<reserva>" desde Carros, "e:<empleado>" desde Empleados). */
@Composable
fun AssignmentDetailRoute(
    key: String,
    onBack: () -> Unit,
    viewModel: AssignmentDetailViewModel = koinViewModel(key = "assignment-detail-$key") { parametersOf(key) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Se desvinculó el último carro: regresa sola a la lista
    LaunchedEffect(state.finished) {
        if (state.finished) onBack()
    }

    AssignmentDetailScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/**
 * Detalle de la asignación (como el de Solicitudes):
 * 1. A quién está asignado (foto y nombre).
 * 2. Cada carro: ícono, datos, desde cuándo, estado y "Desvincular".
 */
@Composable
fun AssignmentDetailScreen(
    state: AssignmentDetailUiState,
    onEvent: (AssignmentDetailEvent) -> Unit,
    onBack: () -> Unit,
) {
    val dimens = AppTheme.dimens
    val detail = state.detail

    AppScreen(maxContentWidth = dimens.maxContentWidth) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppBackButton(onClick = onBack)
            Spacer(Modifier.width(dimens.spaceM))
            Text(
                text = stringResource(Res.string.assignments_detail_title),
                style = AppTheme.typography.title,
                color = AppTheme.colors.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(dimens.spaceL))

        if (state.isOffline) {
            OfflineBanner(stringResource(Res.string.assignments_offline))
            Spacer(Modifier.height(dimens.spaceM))
        }

        when {
            detail == null && state.isLoading -> LoadingView()
            detail == null && state.error != null -> ErrorView(
                message = state.error?.asText().orEmpty(),
                onRetry = { onEvent(AssignmentDetailEvent.Retry) },
            )
            // Ya no existe: se desvinculó (quizá desde otro celular) o el viaje la cerró
            detail == null -> EmptyView(stringResource(Res.string.assignments_detail_not_found))
            else -> Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceM)) {
                HolderCard(employee = detail.employee, vehicles = detail.vehicles.size)
                detail.vehicles.forEach { car ->
                    VehicleCard(
                        car = car,
                        canUnlink = state.canUnlink,
                        onUnlink = {
                            onEvent(
                                AssignmentDetailEvent.UnlinkClicked(
                                    UnlinkTarget(car.assignment.reservationId, car.vehicle.title, car.vehicle.plates, detail.employee.name),
                                ),
                            )
                        },
                    )
                }
                Spacer(Modifier.height(dimens.spaceL))
            }
        }
    }

    Dialogs(state = state, onEvent = onEvent)
}

// ---------------- 1. A quién está asignado ----------------

@Composable
private fun HolderCard(employee: AssignmentEmployee, vehicles: Int) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(dimens.spaceS), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(photoUrl = employee.photoUrl, size = 64.dp, contentDescription = employee.name)
            Spacer(Modifier.width(dimens.spaceM))
            Column(Modifier.weight(1f)) {
                Overline(stringResource(Res.string.assignments_detail_holder))
                Text(
                    text = employee.name,
                    style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.assignments_vehicle_count, vehicles),
                    style = AppTheme.typography.bodySecondary,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

// ---------------- 2. Cada carro con "Desvincular" ----------------

@Composable
private fun VehicleCard(car: EmployeeVehicle, canUnlink: Boolean, onUnlink: () -> Unit) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val vehicle = car.vehicle
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(dimens.spaceS)) {
            Overline(stringResource(Res.string.assignments_detail_vehicle))
            Spacer(Modifier.height(dimens.spaceS))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(vehicle.type.icon, contentDescription = null)
                Spacer(Modifier.width(dimens.spaceM))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = vehicle.title,
                        style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            stringResource(Res.string.assignments_plates_value, vehicle.plates),
                            vehicle.color?.takeIf { it.isNotBlank() },
                        ).joinToString("  ·  "),
                        style = AppTheme.typography.bodySecondary,
                        color = colors.textSecondary,
                    )
                    formatDate(car.assignment.since)?.let {
                        Text(
                            text = stringResource(Res.string.assignments_since, it),
                            style = AppTheme.typography.caption,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(dimens.spaceS))
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
                VehicleStateChip(vehicle.state)
                if (car.assignment.hasActiveTrip) ActiveTripChip()
            }

            Spacer(Modifier.height(dimens.spaceM))
            HorizontalDivider(thickness = 1.dp, color = colors.divider)
            Spacer(Modifier.height(dimens.spaceM))

            // Con viaje en curso no se puede (la API también lo valida) y se explica por qué
            AppButton(
                text = stringResource(Res.string.assignments_unlink),
                onClick = onUnlink,
                variant = AppButtonVariant.Danger,
                enabled = canUnlink && !car.assignment.hasActiveTrip,
                modifier = Modifier.fillMaxWidth(),
            )
            if (car.assignment.hasActiveTrip) {
                Spacer(Modifier.height(dimens.spaceXs))
                Text(
                    text = stringResource(Res.string.assignments_unlink_blocked),
                    style = AppTheme.typography.caption,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Etiqueta pequeña en mayúsculas ("ASIGNADO A", "VEHÍCULO"), como en el detalle de Solicitudes. */
@Composable
private fun Overline(text: String) {
    Text(
        text = text.uppercase(),
        style = AppTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
        color = AppTheme.colors.textSecondary,
    )
}

// ---------------- Diálogos ----------------

@Composable
private fun Dialogs(state: AssignmentDetailUiState, onEvent: (AssignmentDetailEvent) -> Unit) {
    val dismiss = { onEvent(AssignmentDetailEvent.DialogDismissed) }

    state.unlinkTarget?.let { target -> UnlinkReasonDialog(target = target, state = state, onEvent = onEvent) }

    state.unlinked?.let { target ->
        ResultDialog(
            type = ResultType.Success,
            title = stringResource(Res.string.assignments_unlinked_title),
            message = stringResource(Res.string.assignments_unlinked_message, target.vehicleTitle, target.plates, target.employeeName),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = dismiss,
            onDismiss = dismiss,
        )
    }

    // 409 de la API (viaje en curso desde otro celular, o ya se había liberado): se muestra su mensaje
    state.unlinkError?.let { error ->
        ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.assignments_unlink_error_title),
            message = error.asText(),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = dismiss,
            onDismiss = dismiss,
        )
    }
}

/** "¿Desvincular el Nissan Versa (ABC-123) de Ana?" + motivo obligatorio. */
@Composable
private fun UnlinkReasonDialog(target: UnlinkTarget, state: AssignmentDetailUiState, onEvent: (AssignmentDetailEvent) -> Unit) {
    val dimens = AppTheme.dimens
    val colors = AppTheme.colors
    Dialog(onDismissRequest = { onEvent(AssignmentDetailEvent.DialogDismissed) }) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp),
            shape = RoundedCornerShape(dimens.radiusDialog),
            color = colors.background,
        ) {
            Column(Modifier.padding(dimens.spaceL), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.assignments_unlink_question, target.vehicleTitle, target.plates, target.employeeName),
                    style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.title,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(dimens.spaceS))
                Text(
                    text = stringResource(Res.string.assignments_unlink_info),
                    style = AppTheme.typography.bodySecondary,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(dimens.spaceM))
                AppTextField(
                    value = state.unlinkReason,
                    onValueChange = { onEvent(AssignmentDetailEvent.UnlinkReasonChanged(it)) },
                    label = stringResource(Res.string.assignments_unlink_reason),
                    placeholder = stringResource(Res.string.assignments_unlink_reason_hint),
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = state.unlinkReasonError?.asText(),
                    enabled = !state.isSending,
                    singleLine = false,
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(dimens.spaceL))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                    AppButton(
                        text = stringResource(Res.string.action_cancel),
                        onClick = { onEvent(AssignmentDetailEvent.DialogDismissed) },
                        modifier = Modifier.weight(1f),
                        variant = AppButtonVariant.Secondary,
                        enabled = !state.isSending,
                    )
                    AppButton(
                        text = stringResource(Res.string.assignments_unlink),
                        onClick = { onEvent(AssignmentDetailEvent.UnlinkConfirmed) },
                        modifier = Modifier.weight(1f),
                        variant = AppButtonVariant.Danger,
                        isLoading = state.isSending,
                    )
                }
            }
        }
    }
}
