package org.vicpoo.gasolineproject.feature.reservations.presentation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.AppTextButton
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
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
import org.vicpoo.gasolineproject.core.ui.components.UserAvatar
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRequest
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationStatus
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.Overline
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.ResolutionBox
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.StatusDotBadge
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.formatDate
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.formatDateTime
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.resolutionTitle
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
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_offline
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_plates
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_requested_at
import org.vicpoo.gasolineproject.feature.reservations.resources.request_detail_requester
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
    /** false desde "Mis solicitudes": solo se consulta (sin Aceptar / Rechazar). */
    allowDecision: Boolean = true,
    viewModel: ReservationDetailViewModel = koinViewModel(key = "reservation-detail-$reservationId") {
        parametersOf(reservationId)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Al aceptar o rechazar, regresa sola a la lista de solicitudes
    LaunchedEffect(state.decision) {
        if (state.decision != null) onBack()
    }

    ReservationDetailScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack, allowDecision = allowDecision)
}

/**
 * Detalle de la solicitud, en tarjetas:
 * 1. Solicitante (foto, nombre y estado) y cuándo la pidió.
 * 2. Motivo del rechazo / cancelación (si aplica).
 * 3. Vehículo (foto y datos).
 * 4. Licencia de conducir del solicitante.
 * 5. Quién la resolvió y cuándo.
 * Abajo, Aceptar / Rechazar (solo pendientes, con privilegio y si [allowDecision]).
 */
@Composable
fun ReservationDetailScreen(
    state: ReservationDetailUiState,
    onEvent: (ReservationDetailEvent) -> Unit,
    onBack: () -> Unit,
    allowDecision: Boolean = true,
) {
    val dimens = AppTheme.dimens
    val request = state.current

    AppScreen(maxContentWidth = dimens.maxContentWidth) {
        // Flecha y título en una sola línea
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppBackButton(onClick = onBack)
            Spacer(Modifier.width(dimens.spaceM))
            Text(
                text = stringResource(Res.string.request_detail_title),
                style = AppTheme.typography.title,
                color = AppTheme.colors.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(dimens.spaceL))

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
            else -> Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceM)) {
                RequesterCard(request)

                // Si la rechazaron o cancelaron, el motivo va justo debajo
                request.resolutionReason?.takeIf { it.isNotBlank() }?.let { answer ->
                    resolutionTitle(request.status)?.let { titleRes ->
                        ResolutionBox(
                            title = stringResource(titleRes),
                            message = answer,
                            tone = if (request.status == ReservationStatus.APPROVED) AppTheme.colors.primary else AppTheme.colors.danger,
                            showWarning = request.status != ReservationStatus.APPROVED,
                        )
                    }
                }

                VehicleCard(state)
                LicenseCard(state, onEvent)
                ResolverCard(state)

                if (allowDecision && state.canDecide) {
                    Spacer(Modifier.height(dimens.spaceS))
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
                Spacer(Modifier.height(dimens.spaceL))
            }
        }
    }

    Dialogs(state = state, onEvent = onEvent)
}

// ---------------- 1. Solicitante ----------------

/** Foto con insignia del estado, "SOLICITANTE", nombre y estado; abajo la fecha en que la pidió. */
@Composable
private fun RequesterCard(request: ReservationRequest) {
    val dimens = AppTheme.dimens
    val name = request.requesterName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.requests_unknown_requester)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(dimens.spaceS)) {
            PersonRow(
                photoUrl = request.requesterPhotoUrl,
                label = stringResource(Res.string.request_detail_requester),
                name = name,
                badge = statusIcon(request.status),
            ) {
                Spacer(Modifier.height(dimens.spaceXs))
                StatusDotBadge(request.status)
            }
            formatDateTime(request.requestedAt)?.let {
                CardDivider()
                // Solo ícono + fecha: el calendario ya dice que es cuándo se pidió
                DateOnlyRow(contentDescription = stringResource(Res.string.request_detail_requested_at), value = it)
            }
        }
    }
}

/** Insignia sobre la foto: ✓ aprobada, ✕ rechazada / cancelada (null = sin insignia). */
@Composable
private fun statusIcon(status: ReservationStatus): Pair<ImageVector, Color>? {
    val colors = AppTheme.colors
    return when (status) {
        ReservationStatus.APPROVED, ReservationStatus.FINISHED -> AppIcons.BadgeCheck to colors.primary
        ReservationStatus.REJECTED, ReservationStatus.CANCELLED -> AppIcons.BadgeX to colors.danger
        else -> null
    }
}

/** Foto redonda (con insignia opcional) + etiqueta en mayúsculas + nombre en una línea. */
@Composable
private fun PersonRow(
    photoUrl: String?,
    label: String,
    name: String,
    badge: Pair<ImageVector, Color>? = null,
    below: @Composable () -> Unit = {},
) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            UserAvatar(photoUrl = photoUrl, size = 64.dp, contentDescription = name)
            badge?.let { (icon, tint) ->
                Surface(
                    shape = CircleShape,
                    color = colors.background,
                    modifier = Modifier.align(Alignment.BottomEnd).size(22.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.width(AppTheme.dimens.spaceM))
        Column(Modifier.weight(1f)) {
            Overline(label)
            Text(
                text = name,
                style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                color = colors.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            below()
        }
    }
}

// ---------------- 3. Vehículo ----------------

@Composable
private fun VehicleCard(state: ReservationDetailUiState) {
    val dimens = AppTheme.dimens
    val request = state.current ?: return
    val detail = state.detail
    val title = request.vehicleTitle?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.requests_unknown_vehicle)

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            AppImage(
                model = request.photoUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                fallbackIcon = AppIcons.Car,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(dimens.radius)),
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = dimens.spaceS, vertical = dimens.spaceM)) {
                // Sin placas junto al nombre: ya aparecen abajo en "Placas"
                Overline(stringResource(Res.string.request_detail_vehicle))
                Text(
                    text = title,
                    style = AppTheme.typography.title.copy(fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(dimens.spaceS))
                request.plates?.let { InfoRow(stringResource(Res.string.request_detail_plates), it) }
                detail?.vehicleYear?.let { InfoRow(stringResource(Res.string.request_detail_year), it.toString()) }
                detail?.vehicleColor?.takeIf { it.isNotBlank() }?.let { InfoRow(stringResource(Res.string.request_detail_color), it) }
                detail?.vehicleMileageKm?.let {
                    InfoRow(stringResource(Res.string.request_detail_mileage), "${formatKm(it)} km", showDivider = false)
                }
            }
        }
    }
}

// ---------------- 4. Licencia ----------------

/** Licencia del solicitante (el confirmador la revisa antes de prestar el carro). */
@Composable
private fun LicenseCard(state: ReservationDetailUiState, onEvent: (ReservationDetailEvent) -> Unit) {
    val dimens = AppTheme.dimens
    val colors = AppTheme.colors
    val detail = state.detail
    val hasLicense = detail?.requesterHasLicense == true

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(dimens.spaceS)) {
            // Encabezado: ícono + título completo (sin la etiqueta gris, que le quitaba espacio)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Badge, contentDescription = null, tint = colors.title, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(dimens.spaceS))
                Text(
                    text = stringResource(Res.string.request_detail_license),
                    style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.title,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(dimens.spaceM))

            when {
                detail == null -> if (state.isLoading) LoadingView(Modifier.height(120.dp)) else SecondaryText("—")
                !hasLicense -> SecondaryText(stringResource(Res.string.request_detail_license_missing))
                else -> {
                    AppImage(
                        model = detail.requesterLicenseUrl,
                        contentDescription = stringResource(Res.string.request_detail_license),
                        contentScale = ContentScale.Crop,
                        fallbackIcon = AppIcons.FileDocument,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(dimens.radius))
                            .clickable { onEvent(ReservationDetailEvent.LicenseClicked) },
                    )
                    Spacer(Modifier.height(dimens.spaceS))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatDate(detail.requesterLicenseUploadedAt)
                                ?.let { stringResource(Res.string.request_detail_license_uploaded, it) }
                                .orEmpty(),
                            style = AppTheme.typography.bodySecondary,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        AppTextButton(
                            text = stringResource(Res.string.request_detail_view_license),
                            onClick = { onEvent(ReservationDetailEvent.LicenseClicked) },
                        )
                    }
                }
            }
        }
    }
}

// ---------------- 5. Quién resolvió ----------------

/** "RESOLVIÓ" con la foto y el nombre; abajo la fecha de respuesta (solo si ya se resolvió). */
@Composable
private fun ResolverCard(state: ReservationDetailUiState) {
    val request = state.current ?: return
    val detail = state.detail
    val resolvedAt = formatDateTime(request.resolvedAt)
    val name = detail?.confirmerName?.takeIf { it.isNotBlank() }
    if (resolvedAt == null && name == null) return

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(AppTheme.dimens.spaceS)) {
            if (name != null) {
                PersonRow(
                    photoUrl = detail?.confirmerPhotoUrl,
                    label = stringResource(Res.string.request_detail_resolved_by),
                    name = name,
                )
            }
            if (resolvedAt != null) {
                if (name != null) CardDivider()
                DateRow(stringResource(Res.string.request_detail_resolved_at), resolvedAt)
            }
        }
    }
}

// ---------------- Piezas comunes ----------------

@Composable
private fun CardDivider() {
    Spacer(Modifier.height(AppTheme.dimens.spaceM))
    HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.divider)
    Spacer(Modifier.height(AppTheme.dimens.spaceM))
}

/** "Fecha de respuesta ............ 📅 26/10/2026 16:02" */
@Composable
private fun DateOnlyRow(contentDescription: String, value: String) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.Calendar, contentDescription = contentDescription, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(AppTheme.dimens.spaceS))
        Text(value, style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.title)
    }
}

@Composable
private fun DateRow(label: String, value: String) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = AppTheme.typography.bodySecondary, color = colors.textSecondary, modifier = Modifier.weight(1f))
        Icon(AppIcons.Calendar, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(AppTheme.dimens.spaceS))
        Text(value, style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.title)
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
            shape = RoundedCornerShape(dimens.radiusDialog),
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
