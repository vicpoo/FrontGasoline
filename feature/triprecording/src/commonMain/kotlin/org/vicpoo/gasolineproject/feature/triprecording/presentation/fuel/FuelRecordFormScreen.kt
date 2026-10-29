package org.vicpoo.gasolineproject.feature.triprecording.presentation.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.platform.rememberCameraCapture
import org.vicpoo.gasolineproject.core.platform.rememberDocumentPicker
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.AppButton
import org.vicpoo.gasolineproject.core.ui.components.AppButtonVariant
import org.vicpoo.gasolineproject.core.ui.components.AppDropdownField
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.AppTextButton
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
import org.vicpoo.gasolineproject.core.ui.components.DropdownOption
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OptionsBottomSheet
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.ResultDialog
import org.vicpoo.gasolineproject.core.ui.components.ResultType
import org.vicpoo.gasolineproject.core.ui.components.SheetOption
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelField
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecordValidator
import org.vicpoo.gasolineproject.feature.triprecording.domain.SendOutcome
import org.vicpoo.gasolineproject.feature.triprecording.presentation.common.label
import org.vicpoo.gasolineproject.feature.triprecording.resources.Res
import org.vicpoo.gasolineproject.feature.triprecording.resources.action_retry
import org.vicpoo.gasolineproject.feature.triprecording.resources.action_save
import org.vicpoo.gasolineproject.feature.triprecording.resources.action_understood
import org.vicpoo.gasolineproject.feature.triprecording.resources.cd_close
import org.vicpoo.gasolineproject.feature.triprecording.resources.cd_remove_photo
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_add_title
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_amount
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_amount_hint
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_change_receipt
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_cost_center_hint
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_edit_title
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_error_message
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_error_title
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_liters
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_liters_hint
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_not_found
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_receipt_sheet_title
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_rejected_message
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_success_message
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_success_title
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_tank_helper
import org.vicpoo.gasolineproject.feature.triprecording.resources.fuel_upload_receipt
import org.vicpoo.gasolineproject.feature.triprecording.resources.profile_option_camera
import org.vicpoo.gasolineproject.feature.triprecording.resources.profile_option_gallery

@Composable
fun FuelRecordFormRoute(
    recordId: String?,
    onBack: () -> Unit,
    viewModel: FuelRecordFormViewModel = koinViewModel(key = "fuel-form-${recordId ?: "new"}") {
        parametersOf(FuelRecordFormArgs(recordId))
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gallery = rememberDocumentPicker(
        mimeTypes = FuelRecordValidator.ALLOWED_MIME_TYPES.toList(),
        onResult = { viewModel.onEvent(FuelRecordFormEvent.ReceiptPicked(it)) },
    )
    val camera = rememberCameraCapture(onResult = { viewModel.onEvent(FuelRecordFormEvent.ReceiptPicked(it)) })

    FuelRecordFormScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onPickFromGallery = { gallery.launch() },
        onTakePhoto = { camera.launch() },
    )
}

@Composable
fun FuelRecordFormScreen(
    state: FuelRecordFormUiState,
    onEvent: (FuelRecordFormEvent) -> Unit,
    onBack: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    val dimens = AppTheme.dimens
    AppScreen {
        AppBackButton(onClick = onBack)
        Spacer(Modifier.height(dimens.spaceM))
        TitleText(stringResource(if (state.isEdit) Res.string.fuel_edit_title else Res.string.fuel_add_title))
        Spacer(Modifier.height(dimens.spaceL))

        when {
            state.isLoading -> LoadingView()
            state.notFound -> ErrorText(stringResource(Res.string.fuel_not_found))
            else -> {
                // Monto y Litros lado a lado (Figma)
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceM)) {
                    AppTextField(
                        value = state.amount,
                        onValueChange = { onEvent(FuelRecordFormEvent.AmountChanged(it)) },
                        label = stringResource(Res.string.fuel_amount),
                        placeholder = stringResource(Res.string.fuel_amount_hint),
                        errorMessage = state.errors[FuelField.AMOUNT]?.asText(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = state.liters,
                        onValueChange = { onEvent(FuelRecordFormEvent.LitersChanged(it)) },
                        label = stringResource(Res.string.fuel_liters),
                        placeholder = stringResource(Res.string.fuel_liters_hint),
                        errorMessage = state.errors[FuelField.LITERS]?.asText(),
                        helperText = state.tankCapacityText?.let { stringResource(Res.string.fuel_tank_helper, it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(dimens.spaceM))

                if (state.costCenters.isNotEmpty()) {
                    AppDropdownField(
                        options = state.costCenters.map { DropdownOption(it.id, it.label()) },
                        selectedId = state.selectedCostCenterId,
                        onSelect = { onEvent(FuelRecordFormEvent.CostCenterSelected(it)) },
                        placeholder = stringResource(Res.string.fuel_cost_center_hint),
                        errorMessage = state.errors[FuelField.COST_CENTER]?.asText(),
                    )
                    Spacer(Modifier.height(dimens.spaceM))
                }

                ReceiptSection(state = state, onEvent = onEvent)
                Spacer(Modifier.height(dimens.spaceL))

                PrimaryButton(
                    text = stringResource(Res.string.action_save),
                    onClick = { onEvent(FuelRecordFormEvent.Save) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = state.isSaving,
                )
                Spacer(Modifier.height(dimens.spaceL))
            }
        }
    }

    if (state.showReceiptOptions) {
        OptionsBottomSheet(
            title = stringResource(Res.string.fuel_receipt_sheet_title),
            closeDescription = stringResource(Res.string.cd_close),
            onDismiss = { onEvent(FuelRecordFormEvent.ReceiptOptionsDismissed) },
            options = listOf(
                SheetOption(stringResource(Res.string.profile_option_gallery), AppIcons.Gallery) {
                    onEvent(FuelRecordFormEvent.ReceiptOptionsDismissed)
                    onPickFromGallery()
                },
                SheetOption(stringResource(Res.string.profile_option_camera), AppIcons.Camera) {
                    onEvent(FuelRecordFormEvent.ReceiptOptionsDismissed)
                    onTakePhoto()
                },
            ),
        )
    }

    SaveResultDialog(state = state, onEvent = onEvent, onBack = onBack)
}

/** "Subir Recibo" o la foto elegida con su X para quitarla (Figma "Editar"). */
@Composable
private fun ReceiptSection(state: FuelRecordFormUiState, onEvent: (FuelRecordFormEvent) -> Unit) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val preview = state.receiptPreview
    if (preview == null) {
        AppButton(
            text = stringResource(Res.string.fuel_upload_receipt),
            onClick = { onEvent(FuelRecordFormEvent.UploadReceiptClicked) },
            modifier = Modifier.fillMaxWidth(),
            variant = AppButtonVariant.Secondary,
            leadingIcon = AppIcons.Upload,
        )
    } else {
        Box(Modifier.fillMaxWidth()) {
            AppImage(
                model = preview,
                contentDescription = stringResource(Res.string.fuel_receipt_sheet_title),
                contentScale = ContentScale.Crop,
                fallbackIcon = AppIcons.FileDocument,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(dimens.radius)),
            )
            Surface(
                shape = CircleShape,
                color = colors.background,
                shadowElevation = 2.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(dimens.spaceS),
            ) {
                IconButton(onClick = { onEvent(FuelRecordFormEvent.RemoveReceipt) }, modifier = Modifier.size(36.dp)) {
                    Icon(AppIcons.Close, contentDescription = stringResource(Res.string.cd_remove_photo), tint = colors.text)
                }
            }
        }
        AppTextButton(
            text = stringResource(Res.string.fuel_change_receipt),
            onClick = { onEvent(FuelRecordFormEvent.UploadReceiptClicked) },
        )
    }
    state.errors[FuelField.RECEIPT]?.let {
        Spacer(Modifier.height(dimens.spaceXs))
        ErrorText(it.asText())
    }
}

/** "¡Solicitud enviada!" (Entendido) o "¡Error al enviar!" (Reintentar). */
@Composable
private fun SaveResultDialog(
    state: FuelRecordFormUiState,
    onEvent: (FuelRecordFormEvent) -> Unit,
    onBack: () -> Unit,
) {
    val close = {
        onEvent(FuelRecordFormEvent.ResultDismissed)
        onBack()
    }
    when (val outcome = state.outcome) {
        SendOutcome.Sent -> ResultDialog(
            type = ResultType.Success,
            title = stringResource(Res.string.fuel_success_title),
            message = stringResource(Res.string.fuel_success_message),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = close,
            onDismiss = close,
        )
        // Sin red: quedó guardado; "Reintentar" vuelve a intentar y cerrar el diálogo regresa al viaje
        SendOutcome.SavedOffline -> ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.fuel_error_title),
            message = stringResource(Res.string.fuel_error_message),
            buttonText = stringResource(Res.string.action_retry),
            onButtonClick = { onEvent(FuelRecordFormEvent.Retry) },
            onDismiss = close,
        )
        is SendOutcome.Rejected -> ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.fuel_error_title),
            message = stringResource(Res.string.fuel_rejected_message, outcome.message ?: AppError.Server(0, null).asText()),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = { onEvent(FuelRecordFormEvent.ResultDismissed) },
            onDismiss = { onEvent(FuelRecordFormEvent.ResultDismissed) },
        )
        null -> state.failure?.let { error ->
            ResultDialog(
                type = ResultType.Error,
                title = stringResource(Res.string.fuel_error_title),
                message = error.asText(),
                buttonText = stringResource(Res.string.action_understood),
                onButtonClick = { onEvent(FuelRecordFormEvent.ResultDismissed) },
                onDismiss = { onEvent(FuelRecordFormEvent.ResultDismissed) },
            )
        }
    }
}
