package org.vicpoo.gasolineproject.ui.vehicles.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.domain.reservations.VehicleRequester
import org.vicpoo.gasolineproject.domain.vehicles.model.Vehicle
import org.vicpoo.gasolineproject.domain.vehicles.model.toImageModel
import org.vicpoo.gasolineproject.ui.common.components.AppImage
import org.vicpoo.gasolineproject.ui.common.components.AppTextField
import org.vicpoo.gasolineproject.ui.common.components.EditSheet
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.action_cancel
import org.vicpoo.gasolineproject.ui.resources.cd_close
import org.vicpoo.gasolineproject.ui.resources.reservation_reason_hint
import org.vicpoo.gasolineproject.ui.resources.reservation_reason_label
import org.vicpoo.gasolineproject.ui.resources.reservation_sheet_counter
import org.vicpoo.gasolineproject.ui.resources.reservation_sheet_send
import org.vicpoo.gasolineproject.ui.resources.reservation_sheet_subtitle
import org.vicpoo.gasolineproject.ui.resources.reservation_sheet_title
import org.vicpoo.gasolineproject.ui.vehicles.common.icon
import org.vicpoo.gasolineproject.ui.vehicles.common.label

/* Hoja para escribir el motivo al solicitar un vehículo (punto 3.2.3). */

/**
 * Enviar solicitud estilo Airbnb: hoja que sube desde abajo con título grande, el carro que se
 * pide (foto, nombre y placas), el motivo con contador y abajo "Cancelar" / "Enviar solicitud".
 */
@Composable
internal fun RequestReasonSheet(vehicle: Vehicle?, state: VehicleDetailUiState, onEvent: (VehicleDetailEvent) -> Unit) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    EditSheet(
        title = stringResource(Res.string.reservation_sheet_title),
        subtitle = stringResource(Res.string.reservation_sheet_subtitle),
        cancelText = stringResource(Res.string.action_cancel),
        saveText = stringResource(Res.string.reservation_sheet_send),
        onCancel = { onEvent(VehicleDetailEvent.RequestDismissed) },
        onSave = { onEvent(VehicleDetailEvent.SendRequest) },
        isSaving = state.isSendingRequest,
        closeDescription = stringResource(Res.string.cd_close),
    ) {
        // El carro que se está pidiendo
        if (vehicle != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimens.radius),
                color = colors.card,
            ) {
                Row(Modifier.padding(dimens.spaceM), verticalAlignment = Alignment.CenterVertically) {
                    AppImage(
                        model = vehicle.photo?.toImageModel(),
                        fallbackIcon = vehicle.type.icon,
                        contentDescription = vehicle.cardTitle,
                        modifier = Modifier.size(width = 88.dp, height = 72.dp).clip(RoundedCornerShape(dimens.radius)),
                    )
                    Spacer(Modifier.width(dimens.spaceM))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = vehicle.cardTitle,
                            style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.title,
                        )
                        Text(vehicle.plates, style = AppTheme.typography.bodySecondary, color = colors.textSecondary)
                    }
                }
            }
            Spacer(Modifier.height(dimens.spaceL))
        }

        AppTextField(
            value = state.requestReason,
            onValueChange = { onEvent(VehicleDetailEvent.RequestReasonChanged(it)) },
            label = stringResource(Res.string.reservation_reason_label),
            placeholder = stringResource(Res.string.reservation_reason_hint),
            modifier = Modifier.fillMaxWidth(),
            errorMessage = state.requestReasonError?.asText(),
            enabled = !state.isSendingRequest,
            singleLine = false,
            minLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        // Contador como en Airbnb (máx. 255, igual que la API)
        Text(
            text = stringResource(Res.string.reservation_sheet_counter, state.requestReason.length, VehicleRequester.MAX_REASON),
            style = AppTheme.typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.align(Alignment.End),
        )
    }
}
