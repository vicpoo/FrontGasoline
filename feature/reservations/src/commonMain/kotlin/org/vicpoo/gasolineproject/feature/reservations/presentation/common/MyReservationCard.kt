package org.vicpoo.gasolineproject.feature.reservations.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRequest
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationStatus
import org.vicpoo.gasolineproject.feature.reservations.resources.Res
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_approval_note_title
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_cancellation_title
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_reason_label
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_rejection_title
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_requested_on
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_send_failed_title
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_approved
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_cancelled
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_finished
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_not_sent
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_pending
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_rejected
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_send_failed
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_status_unknown
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_unknown_vehicle

/**
 * Tarjeta de "Mis solicitudes" (campana):
 * carro + placas + estado, fecha en que se pidió, el motivo y, si la rechazaron, cancelaron o
 * no se pudo enviar, un recuadro rojo con la respuesta.
 */
@Composable
fun MyReservationCard(request: ReservationRequest, onClick: (() -> Unit)? = null) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val title = request.vehicleTitle?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.requests_unknown_vehicle)

    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(dimens.spaceS)) {
            // Carro + placas + estado
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceS),
                ) {
                    Text(
                        text = title,
                        style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    request.plates?.takeIf { it.isNotBlank() }?.let { PlatesTag(it) }
                }
                Spacer(Modifier.width(dimens.spaceS))
                StatusDotBadge(request.status)
            }

            // Fecha
            formatDate(request.requestedAt)?.let { date ->
                Spacer(Modifier.height(dimens.spaceXs))
                RequestedOn(date)
            }

            // Para qué se pidió
            request.requestReason?.takeIf { it.isNotBlank() }?.let { reason ->
                Spacer(Modifier.height(dimens.spaceM))
                HorizontalDivider(thickness = 1.dp, color = colors.divider)
                Spacer(Modifier.height(dimens.spaceM))
                Overline(stringResource(Res.string.requests_reason_label))
                Spacer(Modifier.height(dimens.spaceXs))
                Text(reason, style = AppTheme.typography.body, color = colors.text)
            }

            // Respuesta de quien la resolvió
            request.resolutionReason?.takeIf { it.isNotBlank() }?.let { answer ->
                resolutionTitle(request.status)?.let { titleRes ->
                    Spacer(Modifier.height(dimens.spaceM))
                    ResolutionBox(
                        title = stringResource(titleRes),
                        message = answer,
                        tone = if (request.status == ReservationStatus.APPROVED) colors.primary else colors.danger,
                        showWarning = request.status != ReservationStatus.APPROVED,
                    )
                }
            }
        }
    }
}

/** Qué título lleva la respuesta según el estado (null = no se muestra). */
private fun resolutionTitle(status: ReservationStatus): StringResource? = when (status) {
    ReservationStatus.REJECTED -> Res.string.requests_rejection_title
    ReservationStatus.CANCELLED -> Res.string.requests_cancellation_title
    ReservationStatus.SEND_FAILED -> Res.string.requests_send_failed_title
    ReservationStatus.APPROVED -> Res.string.requests_approval_note_title
    else -> null
}

/** Placas en una etiqueta gris (como los labels de la guía). */
@Composable
private fun PlatesTag(plates: String) {
    Text(
        text = plates,
        style = AppTheme.typography.caption.copy(fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
        color = AppTheme.colors.onLabel,
        maxLines = 1,
        modifier = Modifier
            .background(AppTheme.colors.labelBackground, RoundedCornerShape(AppTheme.dimens.radius))
            .padding(horizontal = AppTheme.dimens.spaceS, vertical = 2.dp),
    )
}

/** Ícono de calendario + "Solicitada el **26/10/2026**". */
@Composable
private fun RequestedOn(date: String) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.Calendar, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(AppTheme.dimens.spaceS))
        val label = stringResource(Res.string.requests_requested_on)
        Text(
            text = buildAnnotatedString {
                append(label)
                append(' ')
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.text)) { append(date) }
            },
            style = AppTheme.typography.bodySecondary,
            color = colors.textSecondary,
        )
    }
}

/** Texto pequeño en mayúsculas para nombrar una sección ("MOTIVO", "OCTUBRE 2026"). */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = AppTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
        color = AppTheme.colors.textSecondary,
        modifier = modifier,
    )
}

/** Recuadro con la respuesta (rojo con advertencia si la rechazaron; azul si es una nota de aprobación). */
@Composable
private fun ResolutionBox(title: String, message: String, tone: Color, showWarning: Boolean) {
    val dimens = AppTheme.dimens
    val background = tone.copy(alpha = if (AppTheme.colors.isDark) 0.18f else 0.07f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radius),
        color = background,
        border = BorderStroke(1.dp, tone.copy(alpha = 0.3f)),
    ) {
        Row(Modifier.padding(dimens.spaceM)) {
            if (showWarning) {
                Icon(AppIcons.Warning, contentDescription = null, tint = tone, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(dimens.spaceS))
            }
            Column {
                Text(title, style = AppTheme.typography.bodySecondary.copy(fontWeight = FontWeight.SemiBold), color = tone)
                Spacer(Modifier.height(2.dp))
                Text(message, style = AppTheme.typography.bodySecondary, color = tone)
            }
        }
    }
}

/**
 * Estado en una píldora suave con un punto de color (Aprobada azul, Denegada roja, Pendiente gris...).
 * Solo usa colores del tema: el fondo es el mismo color con transparencia.
 */
@Composable
fun StatusDotBadge(status: ReservationStatus) {
    val colors = AppTheme.colors
    val (text, tone) = statusTone(status)
    // Neutros (pendiente, cancelada...): fondo gris de label
    val neutral = tone == null
    val accent = tone ?: colors.onLabel
    val background = if (neutral) colors.labelBackground else accent.copy(alpha = if (colors.isDark) 0.25f else 0.12f)
    val content = if (neutral) colors.onLabel else if (colors.isDark && accent == colors.secondary) colors.title else accent
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(content, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, style = AppTheme.typography.caption.copy(fontWeight = FontWeight.Medium), color = content, maxLines = 1)
    }
}

/** Texto y color de cada estado (null = neutro/gris). */
@Composable
private fun statusTone(status: ReservationStatus): Pair<String, Color?> {
    val colors = AppTheme.colors
    return when (status) {
        ReservationStatus.APPROVED -> stringResource(Res.string.requests_status_approved) to colors.primary
        ReservationStatus.FINISHED -> stringResource(Res.string.requests_status_finished) to colors.secondary
        ReservationStatus.REJECTED -> stringResource(Res.string.requests_status_rejected) to colors.danger
        ReservationStatus.SEND_FAILED -> stringResource(Res.string.requests_status_send_failed) to colors.danger
        ReservationStatus.PENDING -> stringResource(Res.string.requests_status_pending) to null
        ReservationStatus.NOT_SENT -> stringResource(Res.string.requests_status_not_sent) to null
        ReservationStatus.CANCELLED -> stringResource(Res.string.requests_status_cancelled) to null
        ReservationStatus.UNKNOWN -> stringResource(Res.string.requests_status_unknown) to null
    }
}
