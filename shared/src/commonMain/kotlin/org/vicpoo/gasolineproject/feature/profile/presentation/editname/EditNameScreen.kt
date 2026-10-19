package org.vicpoo.gasolineproject.feature.profile.presentation.editname

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.action_retry
import gasolineproject.shared.generated.resources.action_save
import gasolineproject.shared.generated.resources.action_understood
import gasolineproject.shared.generated.resources.edit_name_error_message
import gasolineproject.shared.generated.resources.edit_name_error_title
import gasolineproject.shared.generated.resources.edit_name_hint
import gasolineproject.shared.generated.resources.edit_name_label
import gasolineproject.shared.generated.resources.edit_name_success_message
import gasolineproject.shared.generated.resources.edit_name_success_title
import gasolineproject.shared.generated.resources.edit_name_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.ResultDialog
import org.vicpoo.gasolineproject.core.ui.components.ResultType
import org.vicpoo.gasolineproject.core.ui.components.SubtitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

@Composable
fun EditNameRoute(
    onBack: () -> Unit,
    viewModel: EditNameViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EditNameScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/** Figma: "Nombre" + campo + "Guardar", con diálogos de error / éxito. */
@Composable
fun EditNameScreen(
    state: EditNameUiState,
    onEvent: (EditNameEvent) -> Unit,
    onBack: () -> Unit,
) {
    val dimens = AppTheme.dimens
    val focusManager = LocalFocusManager.current

    AppScreen {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppBackButton(onClick = onBack)
            Spacer(Modifier.width(dimens.spaceM))
            SubtitleText(stringResource(Res.string.edit_name_title))
        }
        Spacer(Modifier.height(dimens.spaceL))

        AppTextField(
            value = state.name,
            onValueChange = { onEvent(EditNameEvent.NameChanged(it)) },
            label = stringResource(Res.string.edit_name_label),
            placeholder = stringResource(Res.string.edit_name_hint),
            modifier = Modifier.fillMaxWidth(),
            errorMessage = state.fieldError?.let { AppError.Validation(it).asText() },
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onEvent(EditNameEvent.Save)
            }),
        )

        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(Res.string.action_save),
            onClick = {
                focusManager.clearFocus()
                onEvent(EditNameEvent.Save)
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isSaving,
        )
    }

    state.saveError?.let { error ->
        ResultDialog(
            type = ResultType.Error,
            title = stringResource(Res.string.edit_name_error_title),
            message = if (error == AppError.NoConnection) stringResource(Res.string.edit_name_error_message) else error.asText(),
            buttonText = stringResource(Res.string.action_retry),
            onButtonClick = { onEvent(EditNameEvent.Save) },
            onDismiss = { onEvent(EditNameEvent.DismissError) },
        )
    }

    if (state.saved) {
        ResultDialog(
            type = ResultType.Success,
            title = stringResource(Res.string.edit_name_success_title),
            message = stringResource(Res.string.edit_name_success_message),
            buttonText = stringResource(Res.string.action_understood),
            onButtonClick = onBack,
            onDismiss = onBack,
        )
    }
}
