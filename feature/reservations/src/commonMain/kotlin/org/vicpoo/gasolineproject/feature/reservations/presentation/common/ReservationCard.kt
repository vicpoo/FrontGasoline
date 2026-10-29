package org.vicpoo.gasolineproject.feature.reservations.presentation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.UserAvatar
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRequest
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationStatus
import org.vicpoo.gasolineproject.feature.reservations.resources.Res
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_card_title
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_unknown_requester

/**
 * Tarjeta de una solicitud.
 * - [showRequester] = true (pestaña llave): SOLO la foto del empleado y "Solicitud de {nombre}";
 *   el carro, el motivo y el estado se ven en el detalle.
 * - false (campana): [MyReservationCard] con el carro, el motivo, el estado y la respuesta.
 * Con [onClick] la tarjeta se puede tocar (abre el detalle).
 */
@Composable
fun ReservationCard(
    request: ReservationRequest,
    showRequester: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    if (!showRequester) {
        MyReservationCard(request, onClick)
        return
    }
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        RequesterHeader(request)
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

/** Etiqueta del estado para el detalle (la misma píldora con punto que la campana). */
@Composable
fun StatusBadge(status: ReservationStatus) = StatusDotBadge(status)

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
