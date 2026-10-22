package org.vicpoo.gasolineproject.feature.triprecording.presentation.finish

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.text.style.TextAlign
import gasolineproject.shared.generated.resources.finish_trip_map_hidden
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.action_retry
import gasolineproject.shared.generated.resources.action_save
import gasolineproject.shared.generated.resources.action_understood
import gasolineproject.shared.generated.resources.cd_close
import gasolineproject.shared.generated.resources.cd_remove_photo
import gasolineproject.shared.generated.resources.finish_trip_change_photo
import gasolineproject.shared.generated.resources.finish_trip_error_message
import gasolineproject.shared.generated.resources.finish_trip_error_title
import gasolineproject.shared.generated.resources.finish_trip_final_mileage
import gasolineproject.shared.generated.resources.finish_trip_final_mileage_hint
import gasolineproject.shared.generated.resources.finish_trip_mileage_helper
import gasolineproject.shared.generated.resources.finish_trip_mileage_too_low
import gasolineproject.shared.generated.resources.finish_trip_no_route
import gasolineproject.shared.generated.resources.finish_trip_not_found
import gasolineproject.shared.generated.resources.finish_trip_photo
import gasolineproject.shared.generated.resources.finish_trip_photo_sheet_title
import gasolineproject.shared.generated.resources.finish_trip_rejected_message
import gasolineproject.shared.generated.resources.finish_trip_route
import gasolineproject.shared.generated.resources.finish_trip_success_message
import gasolineproject.shared.generated.resources.finish_trip_success_title
import gasolineproject.shared.generated.resources.finish_trip_title
import gasolineproject.shared.generated.resources.finish_trip_upload_photo
import gasolineproject.shared.generated.resources.profile_option_camera
import gasolineproject.shared.generated.resources.profile_option_gallery
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.platform.rememberCameraCapture
import org.vicpoo.gasolineproject.core.platform.rememberDocumentPicker
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.AppButton
import org.vicpoo.gasolineproject.core.ui.components.AppButtonVariant
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.AppTextButton
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OptionsBottomSheet
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.ResultDialog
import org.vicpoo.gasolineproject.core.ui.components.ResultType
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.SheetOption
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.map.LiveRouteMap
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.triprecording.domain.FinishTripField
import org.vicpoo.gasolineproject.feature.triprecording.domain.FinishTripValidator
import org.vicpoo.gasolineproject.feature.triprecording.domain.SendOutcome

@Composable
fun FinishTripRoute(
    onBack: () -> Unit,
    /** El viaje ya quedó guardado: regresar a la lista de carros. */
    onDone: () -> Unit,
    viewModel: FinishTripViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gallery = rememberDocumentPicker(
        mimeTypes = FinishTripValidator.ALLOWED_MIME_TYPES.toList(),
        onResult = { viewModel.onEvent(FinishTripEvent.PhotoPicked(it)) },
    )
    val camera = rememberCameraCapture(onResult = { viewModel.onEvent(FinishTripEvent.PhotoPicked(it)) })

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    FinishTripScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onPickFromGallery = { gallery.launch() },
        onTakePhoto = { camera.launch() },
    )
}

@Composable
fun FinishTripScreen(
    state: FinishTripUiState,
    onEvent: (FinishTripEvent) -> Unit,
    onBack: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    val dimens = AppTheme.dimens
    AppScreen {
        AppBackButton(onClick = onBack)
        Spacer(Modifier.height(dimens.spaceM))
        TitleText(stringResource(Res.string.finish_trip_title))
        Spacer(Modifier.height(dimens.spaceL))

        when {
            state.isLoading -> LoadingView()
            state.notFound -> ErrorText(stringResource(Res.string.finish_trip_not_found))
            else -> {
                AppTextField(
                    value = state.finalMileage,
                    onValueChange = { onEvent(FinishTripEvent.FinalMileageChanged(it)) },
                    label = stringResource(Res.string.finish_trip_final_mileage),
                    placeholder = stringResource(Res.string.finish_trip_final_mileage_hint),
                    errorMessage = state.errors[FinishTripField.FINAL_MILEAGE]?.let { reason ->
                        if (reason == ValidationReason.OUT_OF_RANGE) {
                            stringResource(Res.string.finish_trip_mileage_too_low, state.initialMileageText)
                        } else {
                            reason.asText()
                        }
                    },
                    helperText = stringResource(Res.string.finish_trip_mileage_helper, state.initialMileageText, state.gpsDistanceText),
                    suffix = "km",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(dimens.spaceM))

                OdometerPhotoSection(state = state, onEvent = onEvent)
                Spacer(Modifier.height(dimens.spaceM))

                RouteSection(state = state)
                Spacer(Modifier.height(dimens.spaceL))

                PrimaryButton(
                    text = stringResource(Res.string.action_save),
                    onClick = { onEvent(FinishTripEvent.Save) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = state.isSaving,
                )
                Spacer(Modifier.height(dimens.spaceL))
            }
        }
    }

    if (state.showPhotoOptions) {
        OptionsBottomSheet(
            title = stringResource(Res.string.finish_trip_photo_sheet_title),
            closeDescription = stringResource(Res.string.cd_close),
            onDismiss = { onEvent(FinishTripEvent.PhotoOptionsDismissed) },
            options = listOf(
                SheetOption(stringResource(Res.string.profile_option_camera), AppIcons.Camera) {
                    onEvent(FinishTripEvent.PhotoOptionsDismissed)
                    onTakePhoto()
                },
                SheetOption(stringResource(Res.string.profile_option_gallery), AppIcons.Gallery) {
                    onEvent(FinishTripEvent.PhotoOptionsDismissed)
                    onPickFromGallery()
                },
            ),
        )
    }

    SaveResultDialog(state = state, onEvent = onEvent)
}

/** "Foto del odómetro": botón para tomarla o la foto elegida con su X. */
@Composable
private fun OdometerPhotoSection(state: FinishTripUiState, onEvent: (FinishTripEvent) -> Unit) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Text(stringResource(Res.string.finish_trip_photo), style = AppTheme.typography.body, color = colors.title)
    Spacer(Modifier.height(dimens.spaceXs))
    val photo = state.photo
    if (photo == null) {
        AppButton(
            text = stringResource(Res.string.finish_trip_upload_photo),
            onClick = { onEvent(FinishTripEvent.PhotoClicked) },
            modifier = Modifier.fillMaxWidth(),
            variant = AppButtonVariant.Secondary,
            leadingIcon = AppIcons.Camera,
        )
    } else {
        Box(Modifier.fillMaxWidth()) {
            AppImage(
                model = photo.bytes,
                contentDescription = stringResource(Res.string.finish_trip_photo),
                contentScale = ContentScale.Crop,
                fallbackIcon = AppIcons.Camera,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(dimens.radiusM)),
            )
            Surface(
                shape = CircleShape,
                color = colors.background,
                shadowElevation = 2.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(dimens.spaceS),
            ) {
                IconButton(onClick = { onEvent(FinishTripEvent.RemovePhoto) }, modifier = Modifier.size(36.dp)) {
                    Icon(AppIcons.Close, contentDescription = stringResource(Res.string.cd_remove_photo), tint = colors.text)
                }
            }
        }
        AppTextButton(
            text = stringResource(Res.string.finish_trip_change_photo),
            onClick = { onEvent(FinishTripEvent.PhotoClicked) },
        )
    }
    state.errors[FinishTripField.ODOMETER_PHOTO]?.let {
        Spacer(Modifier.height(dimens.spaceXs))
        ErrorText(it.asText())
    }
}

/** "Ruta": el mapa con la ruta que se hizo pintada (Figma). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteSection(state: FinishTripUiState) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Text(stringResource(Res.string.finish_trip_route), style = AppTheme.typography.body, color = colors.title)
    Spacer(Modifier.height(dimens.spaceXs))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(dimens.radiusM))
            .background(colors.labelBackground),
    ) {
        // Con el teclado abierto el mapa (GL) se quita: en Android, redimensionarlo junto con el teclado
        // dentro de una pantalla con scroll congelaba la app ("no responde" / ANR al escribir el kilometraje)
        if (WindowInsets.isImeVisible) {
            SecondaryText(
                stringResource(Res.string.finish_trip_map_hidden),
                modifier = Modifier.align(Alignment.Center).padding(dimens.spaceM),
                textAlign = TextAlign.Center,
            )
        } else {
            LiveRouteMap(
                route = state.route,
                current = state.route.lastOrNull(),
                followCurrent = false,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
    if (state.route.isEmpty()) {
        Spacer(Modifier.height(dimens.spaceXs))
        SecondaryText(stringResource(Res.string.finish_trip_no_route))
    }
}

/**
 * Resultado de guardar (Figma):
 * - Enviado: "¡Viaje registrado!" (Entendido).
 * - Sin red: "¡Error al enviar!" (Reintentar). Quedó guardado y se envía solo después.
 * - Rechazado por el servidor: el mensaje (Entendido) y se queda para corregir el kilometraje.
 */
@Composable
private fun SaveResultDialog(state: FinishTripUiState, onEvent: (FinishTripEvent) -> Unit) {
    val dismiss = { onEvent(FinishTripEvent.ResultDismissed) }
    when (val outcome = state.outcome) {
        SendOutcome.Sent -> ResultDialog(
            type = ResultType.Success,
            title = stringResource(Res.string.finish_trip_success_title),
            message = stringResource(Res.string.finish_trip_success_message),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = dismiss,
            onDismiss = dismiss,
        )
        SendOutcome.SavedOffline -> ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.finish_trip_error_title),
            message = stringResource(Res.string.finish_trip_error_message),
            buttonText = stringResource(Res.string.action_retry),
            onButtonClick = { onEvent(FinishTripEvent.Retry) },
            onDismiss = dismiss,
        )
        is SendOutcome.Rejected -> ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.finish_trip_error_title),
            message = stringResource(
                Res.string.finish_trip_rejected_message,
                outcome.message ?: AppError.Server(0, null).asText(),
            ),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = dismiss,
            onDismiss = dismiss,
        )
        null -> state.failure?.let { error ->
            ResultDialog(
                type = ResultType.Error,
                title = stringResource(Res.string.finish_trip_error_title),
                message = error.asText(),
                buttonText = stringResource(Res.string.action_understood),
                onButtonClick = dismiss,
                onDismiss = dismiss,
            )
        }
    }
}
