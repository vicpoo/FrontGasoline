package org.vicpoo.gasolineproject.feature.triprecording.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.cost_center_value
import gasolineproject.shared.generated.resources.vehicle_plates_value
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicle
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenter
import org.vicpoo.gasolineproject.feature.triprecording.domain.ReceiptPhoto
import kotlin.math.roundToLong

/** Para mostrar la foto del recibo con Coil (URL o archivo local). */
fun ReceiptPhoto.toImageModel(): Any = when (this) {
    is ReceiptPhoto.Remote -> url
    is ReceiptPhoto.Local -> "file://$path"
}

/** "Operaciones central • CC-104" */
@Composable
fun CostCenter.label(): String = stringResource(Res.string.cost_center_value, name, key)

/**
 * Tarjeta del carro asignado (Figma "Registrar" / "Iniciar Viaje"): foto, modelo y placas.
 * [action] es el botón de la derecha (ej. "▷ comienzo"); puede ser null.
 */
@Composable
fun AssignedVehicleCard(
    vehicle: AssignedVehicle,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppImage(
                model = vehicle.photoUrl,
                contentDescription = vehicle.title,
                modifier = Modifier
                    .size(width = 96.dp, height = 72.dp)
                    .clip(RoundedCornerShape(dimens.radiusS)),
            )
            Spacer(Modifier.width(dimens.spaceS))
            Column(Modifier.weight(1f)) {
                Text(
                    text = vehicle.title,
                    style = AppTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                vehicle.color?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = AppTheme.typography.caption, color = colors.textSecondary, maxLines = 1)
                }
                Text(
                    text = stringResource(Res.string.vehicle_plates_value, vehicle.plates),
                    style = AppTheme.typography.caption,
                    color = colors.textSecondary,
                )
                if (action != null) {
                    Spacer(Modifier.height(dimens.spaceXs))
                    Row(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        action()
                    }
                }
            }
        }
    }
}

/** Formatos de la pantalla del viaje. */
object TripFormat {
    /** 65_000 ms -> "01:05"; 3_725_000 ms -> "1:02:05". */
    fun duration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "$hours:${minutes.pad()}:${seconds.pad()}" else "${minutes.pad()}:${seconds.pad()}"
    }

    /** 1234.5 m -> "1.23"; 0 -> "0". */
    fun km(meters: Double): String {
        val hundredths = (meters / 10.0).roundToLong()
        if (hundredths == 0L) return "0"
        val whole = hundredths / 100
        val fraction = (hundredths % 100).toString().padStart(2, '0')
        return "$whole.$fraction"
    }

    /** "2026-09-24T10:32:15" -> "24/09" */
    fun dayMonth(iso: String?): String {
        val match = iso?.let { Regex("""^\d{4}-(\d{2})-(\d{2})""").find(it) } ?: return "—"
        val (month, day) = match.destructured
        return "$day/$month"
    }

    /** 1000.0 -> "1,000"; 450.5 -> "450.50" */
    fun money(value: Double?): String {
        if (value == null) return "—"
        val cents = (value * 100).roundToLong()
        val whole = (cents / 100).toString().reversed().chunked(3).joinToString(",").reversed()
        val fraction = cents % 100
        return if (fraction == 0L) whole else "$whole.${fraction.toString().padStart(2, '0')}"
    }

    /** 10.0 -> "10"; 10.25 -> "10.25" */
    fun number(value: Double): String {
        val rounded = (value * 100).roundToLong() / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    private fun Long.pad() = toString().padStart(2, '0')
}
