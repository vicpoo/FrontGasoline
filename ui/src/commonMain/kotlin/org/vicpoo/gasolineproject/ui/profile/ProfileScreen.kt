package org.vicpoo.gasolineproject.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.platform.files.rememberCameraCapture
import org.vicpoo.gasolineproject.platform.files.rememberDocumentPicker
import org.vicpoo.gasolineproject.ui.common.components.AppCard
import org.vicpoo.gasolineproject.ui.common.components.AppScreen
import org.vicpoo.gasolineproject.ui.common.components.ConfirmDialog
import org.vicpoo.gasolineproject.ui.common.components.EditableAvatar
import org.vicpoo.gasolineproject.ui.common.components.OptionsBottomSheet
import org.vicpoo.gasolineproject.ui.common.components.PrimaryButton
import org.vicpoo.gasolineproject.ui.common.components.ResultDialog
import org.vicpoo.gasolineproject.ui.common.components.ResultType
import org.vicpoo.gasolineproject.ui.common.components.ScreenHeader
import org.vicpoo.gasolineproject.ui.common.components.SecondaryText
import org.vicpoo.gasolineproject.ui.common.components.SheetOption
import org.vicpoo.gasolineproject.ui.common.components.SubtitleText
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.domain.profile.UpdateProfilePhotoUseCase
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.action_logout
import org.vicpoo.gasolineproject.ui.resources.action_retry
import org.vicpoo.gasolineproject.ui.resources.cd_change_photo
import org.vicpoo.gasolineproject.ui.resources.cd_close
import org.vicpoo.gasolineproject.ui.resources.cd_edit_name
import org.vicpoo.gasolineproject.ui.resources.logout_pending_cancel
import org.vicpoo.gasolineproject.ui.resources.logout_pending_confirm
import org.vicpoo.gasolineproject.ui.resources.logout_pending_message
import org.vicpoo.gasolineproject.ui.resources.profile_license_ok
import org.vicpoo.gasolineproject.ui.resources.profile_license_pending
import org.vicpoo.gasolineproject.ui.resources.profile_license_title
import org.vicpoo.gasolineproject.ui.resources.profile_option_camera
import org.vicpoo.gasolineproject.ui.resources.profile_option_gallery
import org.vicpoo.gasolineproject.ui.resources.profile_photo_error_title
import org.vicpoo.gasolineproject.ui.resources.profile_photo_sheet_title
import org.vicpoo.gasolineproject.ui.resources.profile_title
import org.vicpoo.gasolineproject.ui.resources.profile_update_license

@Composable
fun ProfileRoute(
    onEditName: () -> Unit,
    onUpdateLicense: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.LoggedOut -> onLoggedOut()
            }
        }
    }

    val galleryPicker = rememberDocumentPicker(
        mimeTypes = UpdateProfilePhotoUseCase.PICKER_MIME_TYPES,
        onResult = { viewModel.onEvent(ProfileEvent.PhotoPicked(it)) },
    )
    val camera = rememberCameraCapture(onResult = { viewModel.onEvent(ProfileEvent.PhotoPicked(it)) })

    ProfileScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onEditName = onEditName,
        onUpdateLicense = onUpdateLicense,
        onPickFromGallery = galleryPicker::launch,
        onTakePhoto = camera::launch,
    )
}

/**
 * Perfil (persona): cambiar la foto y el nombre del empleado.
 * La licencia SOLO aparece si el usuario maneja (viaje.registrar); a los demás no se les pide.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    onEditName: () -> Unit,
    onUpdateLicense: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    val dimens = AppTheme.dimens
    val profile = state.profile
    val canEdit = profile?.hasEmployee == true

    AppScreen(horizontalAlignment = Alignment.CenterHorizontally) {
        // Mismo título y posición que "Vehículos" y las demás pestañas
        // Salir: ícono a la derecha del título (mismo lugar que la campana en otras pestañas)
        ScreenHeader(
            title = stringResource(Res.string.profile_title),
            topSpacing = 0.dp,
            action = {
                IconButton(onClick = { onEvent(ProfileEvent.Logout) }, enabled = !state.isCheckingLogout) {
                    if (state.isCheckingLogout) {
                        // Mandando lo pendiente antes de salir
                        CircularProgressIndicator(
                            color = AppTheme.colors.title,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Icon(
                            imageVector = AppIcons.Logout,
                            contentDescription = stringResource(Res.string.action_logout),
                            tint = AppTheme.colors.title,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            },
        )
        Spacer(Modifier.height(dimens.spaceL))

        EditableAvatar(
            photoModel = profile?.photoUrl,
            onEditClick = { if (canEdit) onEvent(ProfileEvent.EditPhotoClicked) },
            isLoading = state.isUploadingPhoto,
            editDescription = stringResource(Res.string.cd_change_photo),
        )
        Spacer(Modifier.height(dimens.spaceM))

        // Nombre: al tocarlo se edita
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(dimens.radius))
                .clickable(enabled = canEdit, onClick = onEditName)
                .padding(horizontal = dimens.spaceS, vertical = dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SubtitleText(profile?.name.orEmpty(), textAlign = TextAlign.Center)
            if (canEdit) {
                Spacer(Modifier.width(dimens.spaceS))
                Icon(
                    AppIcons.Pencil,
                    contentDescription = stringResource(Res.string.cd_edit_name),
                    tint = AppTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        profile?.email?.let {
            Spacer(Modifier.height(dimens.spaceM))
            SecondaryText(it, textAlign = TextAlign.Center)
        }

        // Licencia: solo para quien maneja (viaje.registrar)
        if (canEdit && profile?.drives == true) {
            Spacer(Modifier.height(dimens.spaceXl))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (profile.hasLicense) AppIcons.BadgeCheck else AppIcons.BadgeX,
                            contentDescription = null,
                            tint = if (profile.hasLicense) AppTheme.colors.confirm else AppTheme.colors.danger,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(dimens.spaceS))
                        SubtitleText(stringResource(Res.string.profile_license_title))
                    }
                    Spacer(Modifier.height(dimens.spaceXs))
                    SecondaryText(
                        stringResource(if (profile.hasLicense) Res.string.profile_license_ok else Res.string.profile_license_pending),
                    )
                    Spacer(Modifier.height(dimens.spaceM))
                    PrimaryButton(
                        text = stringResource(Res.string.profile_update_license),
                        onClick = onUpdateLicense,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(Modifier.height(dimens.spaceXl))
    }

    if (state.showPhotoOptions) {
        OptionsBottomSheet(
            title = stringResource(Res.string.profile_photo_sheet_title),
            closeDescription = stringResource(Res.string.cd_close),
            onDismiss = { onEvent(ProfileEvent.DismissPhotoOptions) },
            options = listOf(
                SheetOption(stringResource(Res.string.profile_option_gallery), AppIcons.Gallery) {
                    onEvent(ProfileEvent.DismissPhotoOptions)
                    onPickFromGallery()
                },
                SheetOption(stringResource(Res.string.profile_option_camera), AppIcons.Camera) {
                    onEvent(ProfileEvent.DismissPhotoOptions)
                    onTakePhoto()
                },
            ),
        )
    }

    state.logoutPendingCount?.let { count ->
        ConfirmDialog(
            message = stringResource(Res.string.logout_pending_message, count),
            // Mismo patrón que "¿Está seguro de eliminar?": confirmar = la acción, cancelar = quedarse.
            // Tocar fuera o deslizar también cancela, así que nunca se pierde nada por accidente.
            confirmText = stringResource(Res.string.logout_pending_confirm),
            dismissText = stringResource(Res.string.logout_pending_cancel),
            onConfirm = { onEvent(ProfileEvent.LogoutAnyway) },
            onDismiss = { onEvent(ProfileEvent.DismissLogoutWarning) },
            icon = AppIcons.Warning,
        )
    }

    state.photoError?.let { error ->
        ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.profile_photo_error_title),
            message = error.asText(),
            buttonText = stringResource(Res.string.action_retry),
            onButtonClick = { onEvent(ProfileEvent.RetryPhoto) },
            onDismiss = { onEvent(ProfileEvent.DismissPhotoError) },
        )
    }
}
