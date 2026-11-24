package org.vicpoo.gasolineproject.ui.reservations.detail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.domain.reservations.usecase.RejectReservationUseCase
import org.vicpoo.gasolineproject.ui.common.components.AppTextField
import org.vicpoo.gasolineproject.ui.common.components.ConfirmDialog
import org.vicpoo.gasolineproject.ui.common.components.EditSheet
import org.vicpoo.gasolineproject.ui.common.components.ImageViewerDialog
import org.vicpoo.gasolineproject.ui.common.components.ResultDialog
import org.vicpoo.gasolineproject.ui.common.components.ResultType
import org.vicpoo.gasolineproject.ui.common.components.SheetCharCounter
import org.vicpoo.gasolineproject.ui.common.components.SheetSubjectCard
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.action_cancel
import org.vicpoo.gasolineproject.ui.resources.action_no
import org.vicpoo.gasolineproject.ui.resources.action_understood
import org.vicpoo.gasolineproject.ui.resources.action_yes
import org.vicpoo.gasolineproject.ui.resources.cd_close
import org.vicpoo.gasolineproject.ui.resources.company_requests_approve_confirm
import org.vicpoo.gasolineproject.ui.resources.company_requests_error_title
import org.vicpoo.gasolineproject.ui.resources.company_requests_reject
import org.vicpoo.gasolineproject.ui.resources.company_requests_reject_reason
import org.vicpoo.gasolineproject.ui.resources.company_requests_reject_reason_hint
import org.vicpoo.gasolineproject.ui.resources.company_requests_reject_sheet_subtitle
import org.vicpoo.gasolineproject.ui.resources.company_requests_reject_sheet_title
import org.vicpoo.gasolineproject.ui.resources.company_requests_requested_by
import org.vicpoo.gasolineproject.ui.resources.requests_unknown_vehicle

/* Diálogos del detalle de una solicitud: confirmar aceptar y motivo de rechazo (punto 3.2.3). */

@Composable
internal fun Dialogs(state: ReservationDetailUiState, onEvent: (ReservationDetailEvent) -> Unit) {
    val dismiss = { onEvent(ReservationDetailEvent.DialogDismissed) }
    val request = state.current

    if (state.showApproveConfirm && request != null) {
        ConfirmDialog(
            message = stringResource(
                Res.string.company_requests_approve_confirm,
                request.vehicleTitle.orEmpty(),
                request.requesterName.orEmpty(),
            ),
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = { onEvent(ReservationDetailEvent.ApproveConfirmed) },
            onDismiss = dismiss,
            isLoading = state.isSending,
        )
    }

    if (state.showRejectDialog) RejectReasonDialog(state = state, onEvent = onEvent)

    if (state.showLicense) {
        ImageViewerDialog(
            model = state.detail?.requesterLicenseUrl,
            onDismiss = dismiss,
            closeDescription = stringResource(Res.string.cd_close),
        )
    }

    // Con éxito no hay diálogo: la pantalla regresa sola a la lista. Solo se avisan los errores.
    state.decisionError?.let { error ->
        ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.company_requests_error_title),
            message = error.asText(),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = dismiss,
            onDismiss = dismiss,
        )
    }
}

/**
 * Rechazar con el mismo diseño que "Enviar solicitud": hoja que sube desde abajo con el carro pedido
 * (foto, placas y quién lo pidió), el motivo obligatorio con contador (lo ve el solicitante en su
 * campana) y el botón "Rechazar" en rojo.
 */
@Composable
internal fun RejectReasonDialog(state: ReservationDetailUiState, onEvent: (ReservationDetailEvent) -> Unit) {
    val request = state.current
    EditSheet(
        title = stringResource(Res.string.company_requests_reject_sheet_title),
        subtitle = stringResource(Res.string.company_requests_reject_sheet_subtitle),
        cancelText = stringResource(Res.string.action_cancel),
        saveText = stringResource(Res.string.company_requests_reject),
        onCancel = { onEvent(ReservationDetailEvent.DialogDismissed) },
        onSave = { onEvent(ReservationDetailEvent.RejectConfirmed) },
        isSaving = state.isSending,
        closeDescription = stringResource(Res.string.cd_close),
        destructive = true,
    ) {
        if (request != null) {
            SheetSubjectCard(
                title = request.vehicleTitle?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.requests_unknown_vehicle),
                subtitle = request.plates,
                imageModel = request.photoUrl,
                detail = request.requesterName?.takeIf { it.isNotBlank() }
                    ?.let { stringResource(Res.string.company_requests_requested_by, it) },
            )
            Spacer(Modifier.height(AppTheme.dimens.spaceL))
        }

        AppTextField(
            value = state.rejectReason,
            onValueChange = { onEvent(ReservationDetailEvent.RejectReasonChanged(it)) },
            label = stringResource(Res.string.company_requests_reject_reason),
            placeholder = stringResource(Res.string.company_requests_reject_reason_hint),
            modifier = Modifier.fillMaxWidth(),
            errorMessage = state.rejectReasonError?.asText(),
            enabled = !state.isSending,
            singleLine = false,
            minLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        SheetCharCounter(
            length = state.rejectReason.length,
            max = RejectReservationUseCase.MAX_REASON,
            modifier = Modifier.align(Alignment.End),
        )
    }
}
