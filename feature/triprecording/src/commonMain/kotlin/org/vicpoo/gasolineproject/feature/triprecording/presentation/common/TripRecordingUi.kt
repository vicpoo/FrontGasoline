package org.vicpoo.gasolineproject.feature.triprecording.presentation.common

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicle
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenter
import org.vicpoo.gasolineproject.feature.triprecording.domain.ReceiptPhoto
import org.vicpoo.gasolineproject.feature.triprecording.resources.Res
import org.vicpoo.gasolineproject.feature.triprecording.resources.cost_center_value

/** Para mostrar la foto del recibo con Coil (URL o archivo local). */
fun ReceiptPhoto.toImageModel(): Any = when (this) {
    is ReceiptPhoto.Remote -> url
    is ReceiptPhoto.Local -> "file://$path"
}

/** "Operaciones central • CC-104" */
@Composable
fun CostCenter.label(): String = stringResource(Res.string.cost_center_value, name, key)

/**
 * Tarjeta del carro asignado (Figma "Registrar" / "Iniciar Viaje"), estilo limpio (Material 3):
 * - Encabezado tipo list item: ícono en un recuadro suave + nombre y "Color · Placas".
 * - [action] abajo a todo lo ancho (ej. "Comenzar viaje"); puede ser null.
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
        // Margen interno parejo: el encabezado queda centrado en la tarjeta (antes iba pegado arriba)
        Column(Modifier.fillMaxWidth().padding(dimens.spaceS)) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Igual que la tarjeta de Vehículos: carro en un círculo del color principal
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.vehicleType(vehicle.vehicleType), contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(dimens.spaceM))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = vehicle.title,
                        style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(vehicle.color?.takeIf { it.isNotBlank() }, vehicle.plates)
                            .joinToString("  ·  "),
                        style = AppTheme.typography.bodySecondary,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // El botón abajo, a todo lo ancho de la tarjeta
            if (action != null) {
                Spacer(Modifier.height(dimens.spaceM))
                Box(Modifier.fillMaxWidth()) { action() }
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
