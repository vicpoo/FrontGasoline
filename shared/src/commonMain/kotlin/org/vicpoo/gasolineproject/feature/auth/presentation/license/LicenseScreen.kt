package org.vicpoo.gasolineproject.feature.auth.presentation.license

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.action_retry
import gasolineproject.shared.generated.resources.cd_close
import gasolineproject.shared.generated.resources.license_option_camera
import gasolineproject.shared.generated.resources.license_option_file
import gasolineproject.shared.generated.resources.license_sheet_title
import gasolineproject.shared.generated.resources.license_action_upload
import gasolineproject.shared.generated.resources.license_subtitle
import gasolineproject.shared.generated.resources.license_title
import gasolineproject.shared.generated.resources.license_update_subtitle
import gasolineproject.shared.generated.resources.license_update_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.platform.rememberCameraCapture
import org.vicpoo.gasolineproject.core.platform.rememberDocumentPicker
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.AppLabel
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.DangerButton
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.OptionsBottomSheet
import org.vicpoo.gasolineproject.core.ui.components.SheetOption
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.UploadLicenseUseCase

/**
 * isUpdate = false: primer inicio de sesión (obligatorio, sin botón regresar).
 * isUpdate = true:  "Actualizar licencia de conducir" desde el perfil (con botón regresar).
 */
@Composable
fun LicenseRoute(
    onGoToHome: () -> Unit,
    isUpdate: Boolean = false,
    onBack: () -> Unit = {},
    viewModel: LicenseViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LicenseEffect.GoToHome -> onGoToHome()
            }
        }
    }

    val picker = rememberDocumentPicker(
        mimeTypes = UploadLicenseUseCase.ALLOWED_MIME_TYPES,
        onResult = { file -> viewModel.onEvent(LicenseEvent.FilePicked(file)) },
    )
    val camera = rememberCameraCapture(onResult = { file -> viewModel.onEvent(LicenseEvent.FilePicked(file)) })

    LicenseScreen(
        state = state,
        onUploadClick = { viewModel.onEvent(LicenseEvent.UploadClicked) },
        onDismissOptions = { viewModel.onEvent(LicenseEvent.SourceOptionsDismissed) },
        onPickDocument = { picker.launch() },
        onTakePhoto = { camera.launch() },
        onRetry = { viewModel.onEvent(LicenseEvent.Retry) },
        isUpdate = isUpdate,
        onBack = onBack,
    )
}

/** Figma: "Bienvenido — Solo un paso más: sube una foto o PDF de tu licencia de conducir." */
@Composable
fun LicenseScreen(
    state: LicenseUiState,
    onUploadClick: () -> Unit,
    onDismissOptions: () -> Unit,
    onPickDocument: () -> Unit,
    onTakePhoto: () -> Unit,
    onRetry: () -> Unit,
    isUpdate: Boolean = false,
    onBack: () -> Unit = {},
) {
    val dimens = AppTheme.dimens

    AppScreen {
        if (isUpdate) {
            AppBackButton(onClick = onBack)
            Spacer(Modifier.height(dimens.spaceM))
        } else {
            Spacer(Modifier.height(dimens.spaceXxl))
        }
        TitleText(stringResource(if (isUpdate) Res.string.license_update_title else Res.string.license_title))
        Spacer(Modifier.height(dimens.spaceS))
        SecondaryText(stringResource(if (isUpdate) Res.string.license_update_subtitle else Res.string.license_subtitle))
        Spacer(Modifier.height(dimens.spaceXxl))

        PrimaryButton(
            text = stringResource(Res.string.license_action_upload),
            onClick = onUploadClick,
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isUploading,
            leadingIcon = AppIcons.FileDocument,
        )

        state.selectedFileName?.let { name ->
            Spacer(Modifier.height(dimens.spaceM))
            AppLabel(text = name)
        }

        state.error?.let { error ->
            Spacer(Modifier.height(dimens.spaceM))
            ErrorText(error.asText(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            if (state.pendingFile != null) {
                Spacer(Modifier.height(dimens.spaceM))
                DangerButton(
                    text = stringResource(Res.string.action_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // Origen del documento: cámara (foto de la licencia) o archivo (foto o PDF guardado)
    if (state.showSourceOptions) {
        OptionsBottomSheet(
            title = stringResource(Res.string.license_sheet_title),
            closeDescription = stringResource(Res.string.cd_close),
            onDismiss = onDismissOptions,
            options = listOf(
                SheetOption(stringResource(Res.string.license_option_camera), AppIcons.Camera) {
                    onDismissOptions()
                    onTakePhoto()
                },
                SheetOption(stringResource(Res.string.license_option_file), AppIcons.FileDocument) {
                    onDismissOptions()
                    onPickDocument()
                },
            ),
        )
    }
}
