package org.vicpoo.gasolineproject.feature.reservations.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.UserAvatar
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRequest
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationStatus
import org.vicpoo.gasolineproject.feature.reservations.resources.Res
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_card_title
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_date
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_my_reason
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_reason
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_approved
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_cancelled
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_finished
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_not_sent
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_pending
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_rejected
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_send_failed
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_unknown
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_unknown_requester
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_unknown_vehicle

/**
 * Tarjeta de una solicitud.
 * - [showRequester] = true (pestaña llave): SOLO la foto del empleado y "Solicitud de {nombre}";
 *   el carro, el motivo y el estado se ven en el detalle.
 * - false (campana): el carro, para qué lo pidió y su estado.
 * Con [onClick] la tarjeta se puede tocar (abre el detalle).
 */
@Composable
fun ReservationCard(
    request: ReservationRequest,
    showRequester: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val title = request.vehicleTitle ?: stringResource(Res.string.requests_unknown_vehicle)
    if (showRequester) {
        AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
            RequesterHeader(request)
        }
        return
    }
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth()) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ícono de "Solicitudes" (el mismo de la pestaña), en lugar de la foto del carro
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Requests, contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(dimens.spaceM))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTheme.typography.subtitle,
                        color = colors.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    request.plates?.let {
                        Text(text = it, style = AppTheme.typography.subtitle, color = colors.title, maxLines = 1)
                    }
                    formatDate(request.requestedAt)?.let {
                        SecondaryText(stringResource(Res.string.requests_date, it))
                    }
                }
            }

            // En la campana se recuerda para qué se pidió
            request.requestReason?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(dimens.spaceXs))
                SecondaryText(stringResource(Res.string.requests_my_reason, it))
            }

            Spacer(Modifier.height(dimens.spaceS))
            StatusPill(request.status)

            // Si la rechazaron, cancelaron o no se pudo enviar, se muestra el motivo debajo
            if (request.status == ReservationStatus.REJECTED || request.status == ReservationStatus.CANCELLED ||
                request.status == ReservationStatus.SEND_FAILED
            ) {
                request.resolutionReason?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(dimens.spaceXs))
                    SecondaryText(stringResource(Res.string.requests_reason, it))
                }
            }
        }
    }
}

/** Foto de perfil + "Solicitud de Óscar Pérez". */
@Composable
private fun RequesterHeader(request: ReservationRequest) {
    val name = request.requesterName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.requests_unknown_requester)
    Row(verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(photoUrl = request.requesterPhotoUrl, size = 52.dp, contentDescription = name)
        Spacer(Modifier.width(AppTheme.dimens.spaceM))
        Text(
            text = stringResource(Res.string.requests_card_title, name),
            style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Etiqueta chica con el estado (Pendiente, Aprobado, Denegado...), para el detalle. */
@Composable
fun StatusBadge(status: ReservationStatus) {
    val (text, container, content) = statusColors(status)
    Surface(shape = RoundedCornerShape(50), color = container.copy(alpha = 0.15f)) {
        Text(
            text = text,
            style = AppTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = if (status == ReservationStatus.CANCELLED || status == ReservationStatus.UNKNOWN || status == ReservationStatus.NOT_SENT) {
                content
            } else {
                container
            },
            modifier = Modifier.padding(horizontal = AppTheme.dimens.spaceM, vertical = AppTheme.dimens.spaceXs),
        )
    }
}

@Composable
private fun statusColors(status: ReservationStatus): Triple<String, Color, Color> {
    val colors = AppTheme.colors
    return when (status) {
        ReservationStatus.PENDING -> Triple(stringResource(Res.string.requests_status_pending), colors.primary, colors.onPrimary)
        ReservationStatus.APPROVED -> Triple(stringResource(Res.string.requests_status_approved), colors.confirm, colors.onConfirm)
        ReservationStatus.REJECTED -> Triple(stringResource(Res.string.requests_status_rejected), colors.danger, colors.onDanger)
        ReservationStatus.CANCELLED -> Triple(stringResource(Res.string.requests_status_cancelled), colors.labelBackground, colors.textSecondary)
        ReservationStatus.FINISHED -> Triple(stringResource(Res.string.requests_status_finished), colors.secondary, colors.onSecondary)
        ReservationStatus.UNKNOWN -> Triple(stringResource(Res.string.requests_status_unknown), colors.labelBackground, colors.textSecondary)
        ReservationStatus.NOT_SENT -> Triple(stringResource(Res.string.requests_status_not_sent), colors.labelBackground, colors.textSecondary)
        ReservationStatus.SEND_FAILED -> Triple(stringResource(Res.string.requests_status_send_failed), colors.danger, colors.onDanger)
    }
}

/** Barra de color con el estado (Pendiente azul, Denegado rojo...). */
@Composable
fun StatusPill(status: ReservationStatus) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val (text, container, content) = when (status) {
        ReservationStatus.PENDING -> Triple(stringResource(Res.string.requests_status_pending), colors.primary, colors.onPrimary)
        ReservationStatus.APPROVED -> Triple(stringResource(Res.string.requests_status_approved), colors.confirm, colors.onConfirm)
        ReservationStatus.REJECTED -> Triple(stringResource(Res.string.requests_status_rejected), colors.danger, colors.onDanger)
        ReservationStatus.CANCELLED -> Triple(stringResource(Res.string.requests_status_cancelled), colors.labelBackground, colors.onLabel)
        ReservationStatus.FINISHED -> Triple(stringResource(Res.string.requests_status_finished), colors.secondary, colors.onSecondary)
        ReservationStatus.UNKNOWN -> Triple(stringResource(Res.string.requests_status_unknown), colors.labelBackground, colors.onLabel)
        ReservationStatus.NOT_SENT -> Triple(stringResource(Res.string.requests_status_not_sent), colors.labelBackground, colors.onLabel)
        ReservationStatus.SEND_FAILED -> Triple(stringResource(Res.string.requests_status_send_failed), colors.danger, colors.onDanger)
    }
    StatusBar(text = text, container = container, content = content, radius = dimens.radiusS)
}

@Composable
private fun StatusBar(text: String, container: Color, content: Color, radius: Dp) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(radius), color = container) {
        Text(
            text = text,
            style = AppTheme.typography.body.copy(fontWeight = FontWeight.Medium),
            color = content,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = AppTheme.dimens.spaceXs),
        )
    }
}

/** "2026-09-24T10:32:15" -> "24/09/2026". */
fun formatDate(iso: String?): String? {
    val match = iso?.let { Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(it) } ?: return null
    val (year, month, day) = match.destructured
    return "$day/$month/$year"
}

/** "2026-09-24T10:32:15" -> "24/09/2026 10:32". */
fun formatDateTime(iso: String?): String? {
    val match = iso?.let { Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})""").find(it) } ?: return null
    val (year, month, day, hour, minute) = match.destructured
    return "$day/$month/$year $hour:$minute"
}
