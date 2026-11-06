package org.vicpoo.gasolineproject.ui.reservations.assignmentdetail

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import org.vicpoo.gasolineproject.ui.common.components.AppBackButton
import org.vicpoo.gasolineproject.ui.common.components.AppButton
import org.vicpoo.gasolineproject.ui.common.components.AppButtonVariant
import org.vicpoo.gasolineproject.ui.common.components.AppCard
import org.vicpoo.gasolineproject.ui.common.components.AppImage
import org.vicpoo.gasolineproject.ui.common.components.AppScreen
import org.vicpoo.gasolineproject.ui.common.components.AppTextField
import org.vicpoo.gasolineproject.ui.common.components.EmptyView
import org.vicpoo.gasolineproject.ui.common.components.ErrorView
import org.vicpoo.gasolineproject.ui.common.components.LoadingView
import org.vicpoo.gasolineproject.ui.common.components.OfflineBanner
import org.vicpoo.gasolineproject.ui.common.components.ResultDialog
import org.vicpoo.gasolineproject.ui.common.components.ResultType
import org.vicpoo.gasolineproject.ui.common.components.UserAvatar
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.common.components.EditSheet
import org.vicpoo.gasolineproject.ui.common.components.SheetCharCounter
import org.vicpoo.gasolineproject.ui.common.components.SheetSubjectCard
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.domain.reservations.UnlinkAssignmentUseCase
import org.vicpoo.gasolineproject.ui.resources.assignments_unlink_sheet_title
import org.vicpoo.gasolineproject.ui.resources.assignments_holder_value
import org.vicpoo.gasolineproject.ui.resources.cd_close
import org.vicpoo.gasolineproject.domain.reservations.AssignmentEmployee
import org.vicpoo.gasolineproject.domain.reservations.EmployeeVehicle
import org.vicpoo.gasolineproject.ui.reservations.assignments.ActiveTripChip
import org.vicpoo.gasolineproject.ui.reservations.assignments.VehicleStateChip
import org.vicpoo.gasolineproject.ui.reservations.assignments.icon
import org.vicpoo.gasolineproject.ui.reservations.common.formatDate
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.action_cancel
import org.vicpoo.gasolineproject.ui.resources.action_understood
import org.vicpoo.gasolineproject.ui.resources.assignments_detail_holder
import org.vicpoo.gasolineproject.ui.resources.assignments_detail_not_found
import org.vicpoo.gasolineproject.ui.resources.assignments_detail_title
import org.vicpoo.gasolineproject.ui.resources.assignments_detail_vehicle
import org.vicpoo.gasolineproject.ui.resources.assignments_offline
import org.vicpoo.gasolineproject.ui.resources.assignments_plates_value
import org.vicpoo.gasolineproject.ui.resources.assignments_since
import org.vicpoo.gasolineproject.ui.resources.assignments_unlink
import org.vicpoo.gasolineproject.ui.resources.assignments_unlink_blocked
import org.vicpoo.gasolineproject.ui.resources.assignments_unlink_error_title
import org.vicpoo.gasolineproject.ui.resources.assignments_unlink_info
import org.vicpoo.gasolineproject.ui.resources.assignments_unlink_question
import org.vicpoo.gasolineproject.ui.resources.assignments_unlink_reason
import org.vicpoo.gasolineproject.ui.resources.assignments_unlink_reason_hint
import org.vicpoo.gasolineproject.ui.resources.assignments_unlinked_message
import org.vicpoo.gasolineproject.ui.resources.assignments_unlinked_title
import org.vicpoo.gasolineproject.ui.resources.assignments_vehicle_count

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
 * 2. Cada carro: foto (como en el detalle de Solicitudes), datos, desde cuándo, estado y "Desvincular".
 *    En la lista no hay fotos (solo íconos); la foto se ve aquí, en el detalle.
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
        Column(Modifier.fillMaxWidth()) {
            // Foto del vehículo; si no tiene, el ícono de su tipo
            AppImage(
                model = vehicle.photoUrl,
                contentDescription = vehicle.title,
                contentScale = ContentScale.Crop,
                fallbackIcon = vehicle.type.icon,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(dimens.radius)),
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = dimens.spaceS, vertical = dimens.spaceM)) {
                Overline(stringResource(Res.string.assignments_detail_vehicle))
                Spacer(Modifier.height(dimens.spaceXs))
                Row(verticalAlignment = Alignment.CenterVertically) {
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

/**
 * Desvincular con el mismo diseño que "Enviar solicitud": hoja que sube desde abajo con el carro
 * (foto, placas y a quién está asignado), el motivo obligatorio con contador y el botón en rojo.
 */
@Composable
private fun UnlinkReasonDialog(target: UnlinkTarget, state: AssignmentDetailUiState, onEvent: (AssignmentDetailEvent) -> Unit) {
    val car = state.detail?.vehicles?.firstOrNull { it.assignment.reservationId == target.reservationId }
    EditSheet(
        title = stringResource(Res.string.assignments_unlink_sheet_title),
        subtitle = stringResource(Res.string.assignments_unlink_info),
        cancelText = stringResource(Res.string.action_cancel),
        saveText = stringResource(Res.string.assignments_unlink),
        onCancel = { onEvent(AssignmentDetailEvent.DialogDismissed) },
        onSave = { onEvent(AssignmentDetailEvent.UnlinkConfirmed) },
        isSaving = state.isSending,
        closeDescription = stringResource(Res.string.cd_close),
        destructive = true,
    ) {
        SheetSubjectCard(
            title = target.vehicleTitle,
            subtitle = target.plates,
            imageModel = car?.vehicle?.photoUrl,
            fallbackIcon = car?.vehicle?.type?.icon ?: AppIcons.Car,
            detail = stringResource(Res.string.assignments_holder_value, target.employeeName),
        )
        Spacer(Modifier.height(AppTheme.dimens.spaceL))

        AppTextField(
            value = state.unlinkReason,
            onValueChange = { onEvent(AssignmentDetailEvent.UnlinkReasonChanged(it)) },
            label = stringResource(Res.string.assignments_unlink_reason),
            placeholder = stringResource(Res.string.assignments_unlink_reason_hint),
            modifier = Modifier.fillMaxWidth(),
            errorMessage = state.unlinkReasonError?.asText(),
            enabled = !state.isSending,
            singleLine = false,
            minLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        SheetCharCounter(
            length = state.unlinkReason.length,
            max = UnlinkAssignmentUseCase.MAX_REASON,
            modifier = Modifier.align(Alignment.End),
        )
    }
}
