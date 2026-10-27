package org.vicpoo.gasolineproject.feature.reservations.presentation.detail

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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.AppTextButton
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
import org.vicpoo.gasolineproject.core.ui.components.BodyText
import org.vicpoo.gasolineproject.core.ui.components.ConfirmDialog
import org.vicpoo.gasolineproject.core.ui.components.ErrorView
import org.vicpoo.gasolineproject.core.ui.components.ImageViewerDialog
import org.vicpoo.gasolineproject.core.ui.components.InfoRow
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OfflineBanner
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.ResultDialog
import org.vicpoo.gasolineproject.core.ui.components.ResultType
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.SubtitleText
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.UserAvatar
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.StatusBadge
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.formatDate
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.formatDateTime
import org.vicpoo.gasolineproject.feature.reservations.resources.Res
import org.vicpoo.gasolineproject.feature.reservations.resources.action_cancel
import org.vicpoo.gasolineproject.feature.reservations.resources.action_no
import org.vicpoo.gasolineproject.feature.reservations.resources.action_understood
import org.vicpoo.gasolineproject.feature.reservations.resources.action_yes
import org.vicpoo.gasolineproject.feature.reservations.resources.cd_close
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_approve
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_approve_confirm
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_approved_message
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_approved_title
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_error_title
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_reject
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_reject_reason
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_reject_reason_hint
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_rejected_message
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_rejected_title
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_color
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_license
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_license_missing
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_license_uploaded
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_mileage
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_no_reason
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_offline
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_plates
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_reason
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_requested_at
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_resolution_reason
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_resolved_at
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_resolved_by
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_title
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_vehicle
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_view_license
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_year
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_unknown_requester
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_unknown_vehicle

@Composable
fun ReservationDetailRoute(
    reservationId: String,
    onBack: () -> Unit,
    viewModel: ReservationDetailViewModel = koinViewModel(key = "reservation-detail-$reservationId") {
        parametersOf(reservationId)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Al aceptar o rechazar, regresa sola a la lista de solicitudes
    LaunchedEffect(state.decision) {
        if (state.decision != null) onBack()
    }

    ReservationDetailScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/** Detalle de la solicitud: quién la pide (foto y licencia), el motivo, el carro y Aceptar / Rechazar. */
@Composable
fun ReservationDetailScreen(
    state: ReservationDetailUiState,
    onEvent: (ReservationDetailEvent) -> Unit,
    onBack: () -> Unit,
) {
    val dimens = AppTheme.dimens
    val request = state.current

    AppScreen(maxContentWidth = dimens.maxContentWidth) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppBackButton(onClick = onBack)
            Spacer(Modifier.width(dimens.spaceM))
            TitleText(stringResource(Res.string.request_detail_title))
        }
        Spacer(Modifier.height(dimens.spaceM))

        if (state.isOffline) {
            OfflineBanner(stringResource(Res.string.request_detail_offline))
            Spacer(Modifier.height(dimens.spaceM))
        }

        when {
            request == null && state.isLoading -> LoadingView()
            request == null -> ErrorView(
                message = state.error?.asText().orEmpty(),
                onRetry = { onEvent(ReservationDetailEvent.Retry) },
            )
            else -> {
                RequesterSection(state)
                Spacer(Modifier.height(dimens.spaceL))

                // Motivo de la solicitud
                SubtitleText(stringResource(Res.string.request_detail_reason))
                Spacer(Modifier.height(dimens.spaceXs))
                BodyText(request.requestReason?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.request_detail_no_reason))
                Spacer(Modifier.height(dimens.spaceS))
                formatDateTime(request.requestedAt)?.let {
                    InfoRow(stringResource(Res.string.request_detail_requested_at), it, showDivider = false)
                }
                Spacer(Modifier.height(dimens.spaceL))

                LicenseSection(state, onEvent)
                Spacer(Modifier.height(dimens.spaceL))

                VehicleSection(state)
                Spacer(Modifier.height(dimens.spaceL))

                ResolutionSection(state)

                if (state.canDecide) {
                    Spacer(Modifier.height(dimens.spaceL))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                        if (state.canReject) {
                            AppButton(
                                text = stringResource(Res.string.company_requests_reject),
                                onClick = { onEvent(ReservationDetailEvent.RejectClicked) },
                                modifier = Modifier.weight(1f),
                                variant = AppButtonVariant.Danger,
                            )
                        }
                        if (state.canApprove) {
                            AppButton(
                                text = stringResource(Res.string.company_requests_approve),
                                onClick = { onEvent(ReservationDetailEvent.ApproveClicked) },
                                modifier = Modifier.weight(1f),
                                variant = AppButtonVariant.Confirm,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(dimens.spaceXl))
            }
        }
    }

    Dialogs(state = state, onEvent = onEvent)
}

/** Arriba y centrado: foto grande, nombre y una etiqueta chica con el estado. */
@Composable
private fun RequesterSection(state: ReservationDetailUiState) {
    val request = state.current ?: return
    val name = request.requesterName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.requests_unknown_requester)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        UserAvatar(photoUrl = request.requesterPhotoUrl, size = 96.dp, contentDescription = name)
        Spacer(Modifier.height(AppTheme.dimens.spaceM))
        SubtitleText(name, textAlign = TextAlign.Center)
        Spacer(Modifier.height(AppTheme.dimens.spaceS))
        StatusBadge(request.status)
    }
}

/** Licencia del solicitante (el confirmador la revisa antes de prestar el carro). */
@Composable
private fun LicenseSection(state: ReservationDetailUiState, onEvent: (ReservationDetailEvent) -> Unit) {
    val dimens = AppTheme.dimens
    val detail = state.detail
    SubtitleText(stringResource(Res.string.request_detail_license))
    Spacer(Modifier.height(dimens.spaceXs))
    when {
        detail == null -> if (state.isLoading) LoadingView(Modifier.height(80.dp)) else SecondaryText("—")
        !detail.requesterHasLicense -> SecondaryText(stringResource(Res.string.request_detail_license_missing))
        else -> {
            AppCard(modifier = Modifier.fillMaxWidth(), onClick = { onEvent(ReservationDetailEvent.LicenseClicked) }) {
                Column(Modifier.fillMaxWidth()) {
                    AppImage(
                        model = detail.requesterLicenseUrl,
                        contentDescription = stringResource(Res.string.request_detail_license),
                        contentScale = ContentScale.Crop,
                        fallbackIcon = AppIcons.FileDocument,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(dimens.radiusS)),
                    )
                    Spacer(Modifier.height(dimens.spaceXs))
                    formatDate(detail.requesterLicenseUploadedAt)?.let {
                        SecondaryText(stringResource(Res.string.request_detail_license_uploaded, it))
                    }
                    AppTextButton(
                        text = stringResource(Res.string.request_detail_view_license),
                        onClick = { onEvent(ReservationDetailEvent.LicenseClicked) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleSection(state: ReservationDetailUiState) {
    val dimens = AppTheme.dimens
    val request = state.current ?: return
    val detail = state.detail
    val title = request.vehicleTitle ?: stringResource(Res.string.requests_unknown_vehicle)

    SubtitleText(stringResource(Res.string.request_detail_vehicle))
    Spacer(Modifier.height(dimens.spaceS))
    AppImage(
        model = request.photoUrl,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        fallbackIcon = AppIcons.Car,
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(dimens.radiusM)),
    )
    Spacer(Modifier.height(dimens.spaceS))
    BodyText(title)
    request.plates?.let { InfoRow(stringResource(Res.string.request_detail_plates), it) }
    detail?.vehicleYear?.let { InfoRow(stringResource(Res.string.request_detail_year), it.toString()) }
    detail?.vehicleColor?.takeIf { it.isNotBlank() }?.let { InfoRow(stringResource(Res.string.request_detail_color), it) }
    detail?.vehicleMileageKm?.let { InfoRow(stringResource(Res.string.request_detail_mileage), "${formatKm(it)} km", showDivider = false) }
}

/** Quién y cuándo la aceptó / rechazó (solo si ya se resolvió). */
@Composable
private fun ResolutionSection(state: ReservationDetailUiState) {
    val request = state.current ?: return
    val detail = state.detail
    val resolvedAt = formatDateTime(request.resolvedAt)
    if (resolvedAt == null && detail?.confirmerName == null) return

    Column(Modifier.fillMaxWidth()) {
        detail?.confirmerName?.let { name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(photoUrl = detail.confirmerPhotoUrl, size = 36.dp, contentDescription = name)
                Spacer(Modifier.width(AppTheme.dimens.spaceS))
                InfoRow(stringResource(Res.string.request_detail_resolved_by), name, modifier = Modifier.weight(1f))
            }
        }
        resolvedAt?.let { InfoRow(stringResource(Res.string.request_detail_resolved_at), it) }
        request.resolutionReason?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.request_detail_resolution_reason), it, showDivider = false)
        }
    }
}

@Composable
private fun Dialogs(state: ReservationDetailUiState, onEvent: (ReservationDetailEvent) -> Unit) {
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

/** "Motivo del rechazo" (obligatorio: lo ve quien pidió el carro en su campana). */
@Composable
private fun RejectReasonDialog(state: ReservationDetailUiState, onEvent: (ReservationDetailEvent) -> Unit) {
    val dimens = AppTheme.dimens
    Dialog(onDismissRequest = { onEvent(ReservationDetailEvent.DialogDismissed) }) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp),
            shape = RoundedCornerShape(dimens.radiusM * 2),
            color = AppTheme.colors.background,
        ) {
            Column(Modifier.padding(dimens.spaceL), horizontalAlignment = Alignment.CenterHorizontally) {
                AppTextField(
                    value = state.rejectReason,
                    onValueChange = { onEvent(ReservationDetailEvent.RejectReasonChanged(it)) },
                    label = stringResource(Res.string.company_requests_reject_reason),
                    placeholder = stringResource(Res.string.company_requests_reject_reason_hint),
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = state.rejectReasonError?.asText(),
                    enabled = !state.isSending,
                    singleLine = false,
                    minLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(dimens.spaceL))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                    AppButton(
                        text = stringResource(Res.string.action_cancel),
                        onClick = { onEvent(ReservationDetailEvent.DialogDismissed) },
                        modifier = Modifier.weight(1f),
                        variant = AppButtonVariant.Secondary,
                    )
                    PrimaryButton(
                        text = stringResource(Res.string.company_requests_reject),
                        onClick = { onEvent(ReservationDetailEvent.RejectConfirmed) },
                        isLoading = state.isSending,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** 234245.0 -> "234,245"; 1234.5 -> "1,234.5". */
private fun formatKm(value: Double): String {
    val whole = value.toLong()
    val decimals = ((value - whole) * 10).toLong()
    val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return if (decimals == 0L) grouped else "$grouped.$decimals"
}
