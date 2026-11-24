package org.vicpoo.gasolineproject.ui.reservations.detail

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.domain.reservations.ReservationStatus
import org.vicpoo.gasolineproject.ui.common.components.AppBackButton
import org.vicpoo.gasolineproject.ui.common.components.AppButton
import org.vicpoo.gasolineproject.ui.common.components.AppButtonVariant
import org.vicpoo.gasolineproject.ui.common.components.AppScreen
import org.vicpoo.gasolineproject.ui.common.components.ErrorView
import org.vicpoo.gasolineproject.ui.common.components.LoadingView
import org.vicpoo.gasolineproject.ui.common.components.OfflineBanner
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.reservations.common.ResolutionBox
import org.vicpoo.gasolineproject.ui.reservations.common.resolutionTitle
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.company_requests_approve
import org.vicpoo.gasolineproject.ui.resources.company_requests_reject
import org.vicpoo.gasolineproject.ui.resources.request_detail_offline
import org.vicpoo.gasolineproject.ui.resources.request_detail_title

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
