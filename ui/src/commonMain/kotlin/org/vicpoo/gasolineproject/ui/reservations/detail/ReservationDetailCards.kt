package org.vicpoo.gasolineproject.ui.reservations.detail

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.domain.reservations.ReservationRequest
import org.vicpoo.gasolineproject.domain.reservations.ReservationStatus
import org.vicpoo.gasolineproject.ui.common.components.AppCard
import org.vicpoo.gasolineproject.ui.common.components.AppImage
import org.vicpoo.gasolineproject.ui.common.components.AppTextButton
import org.vicpoo.gasolineproject.ui.common.components.InfoRow
import org.vicpoo.gasolineproject.ui.common.components.LoadingView
import org.vicpoo.gasolineproject.ui.common.components.SecondaryText
import org.vicpoo.gasolineproject.ui.common.components.UserAvatar
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.reservations.common.Overline
import org.vicpoo.gasolineproject.ui.reservations.common.StatusDotBadge
import org.vicpoo.gasolineproject.ui.reservations.common.formatDate
import org.vicpoo.gasolineproject.ui.reservations.common.formatDateTime
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.request_detail_color
import org.vicpoo.gasolineproject.ui.resources.request_detail_license
import org.vicpoo.gasolineproject.ui.resources.request_detail_license_missing
import org.vicpoo.gasolineproject.ui.resources.request_detail_license_uploaded
import org.vicpoo.gasolineproject.ui.resources.request_detail_mileage
import org.vicpoo.gasolineproject.ui.resources.request_detail_plates
import org.vicpoo.gasolineproject.ui.resources.request_detail_requested_at
import org.vicpoo.gasolineproject.ui.resources.request_detail_requester
import org.vicpoo.gasolineproject.ui.resources.request_detail_resolved_at
import org.vicpoo.gasolineproject.ui.resources.request_detail_resolved_by
import org.vicpoo.gasolineproject.ui.resources.request_detail_vehicle
import org.vicpoo.gasolineproject.ui.resources.request_detail_view_license
import org.vicpoo.gasolineproject.ui.resources.request_detail_year
import org.vicpoo.gasolineproject.ui.resources.requests_unknown_requester
import org.vicpoo.gasolineproject.ui.resources.requests_unknown_vehicle

/* Tarjetas del detalle de una solicitud: solicitante, vehículo, licencia y quién resolvió (separadas de ReservationDetailScreen, punto 3.2.3). */

/** Foto con insignia del estado, "SOLICITANTE", nombre y estado; abajo la fecha en que la pidió. */
@Composable
internal fun RequesterCard(request: ReservationRequest) {
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
internal fun statusIcon(status: ReservationStatus): Pair<ImageVector, Color>? {
    val colors = AppTheme.colors
    return when (status) {
        ReservationStatus.APPROVED, ReservationStatus.FINISHED -> AppIcons.BadgeCheck to colors.primary
        ReservationStatus.REJECTED, ReservationStatus.CANCELLED -> AppIcons.BadgeX to colors.danger
        else -> null
    }
}

/** Foto redonda (con insignia opcional) + etiqueta en mayúsculas + nombre en una línea. */
@Composable
internal fun PersonRow(
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

@Composable
internal fun VehicleCard(state: ReservationDetailUiState) {
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

/** Licencia del solicitante (el confirmador la revisa antes de prestar el carro). */
@Composable
internal fun LicenseCard(state: ReservationDetailUiState, onEvent: (ReservationDetailEvent) -> Unit) {
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

/** "RESOLVIÓ" con la foto y el nombre; abajo la fecha de respuesta (solo si ya se resolvió). */
@Composable
internal fun ResolverCard(state: ReservationDetailUiState) {
    val request = state.current ?: return
    val detail = state.detail
    val resolvedAt = formatDateTime(request.resolvedAt)
    val name = detail?.confirmerName?.takeIf { it.isNotBlank() }
    if (resolvedAt == null && name == null) return

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(AppTheme.dimens.spaceS)) {
            if (name != null) {
                PersonRow(
                    photoUrl = detail.confirmerPhotoUrl,
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

@Composable
internal fun CardDivider() {
    Spacer(Modifier.height(AppTheme.dimens.spaceM))
    HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.divider)
    Spacer(Modifier.height(AppTheme.dimens.spaceM))
}

/** "Fecha de respuesta ............ 📅 26/10/2026 16:02" */
@Composable
internal fun DateOnlyRow(contentDescription: String, value: String) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.Calendar, contentDescription = contentDescription, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(AppTheme.dimens.spaceS))
        Text(value, style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.title)
    }
}

@Composable
internal fun DateRow(label: String, value: String) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = AppTheme.typography.bodySecondary, color = colors.textSecondary, modifier = Modifier.weight(1f))
        Icon(AppIcons.Calendar, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(AppTheme.dimens.spaceS))
        Text(value, style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.title)
    }
}

/** 234245.0 -> "234,245"; 1234.5 -> "1,234.5". */
internal fun formatKm(value: Double): String {
    val whole = value.toLong()
    val decimals = ((value - whole) * 10).toLong()
    val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return if (decimals == 0L) grouped else "$grouped.$decimals"
}
