package org.vicpoo.gasolineproject.feature.auth.presentation.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.auth.resources.Res
import org.vicpoo.gasolineproject.feature.auth.resources.login_action
import org.vicpoo.gasolineproject.feature.auth.resources.login_password_label
import org.vicpoo.gasolineproject.feature.auth.resources.login_title
import org.vicpoo.gasolineproject.feature.auth.resources.login_user_label

/** Punto de entrada desde la navegación: conecta el ViewModel con la UI. */
@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onGoToUploadLicense: () -> Unit,
    onGoToHome: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.GoToUploadLicense -> onGoToUploadLicense()
                LoginEffect.GoToHome -> onGoToHome()
            }
        }
    }

    LoginScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/** UI pura: solo dibuja el estado y manda eventos (fácil de previsualizar y probar). */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    onBack: () -> Unit,
) {
    val dimens = AppTheme.dimens
    val focusManager = LocalFocusManager.current

    AppScreen {
        AppBackButton(onClick = onBack)
        // Los dos Spacer con peso centran el formulario en el alto disponible (sin espacio muerto
        // abajo). El de abajo pesa un poco más para que quede ópticamente centrado, no caído.
        Spacer(Modifier.weight(1f))
        TitleText(
            text = stringResource(Res.string.login_title),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(dimens.spaceXl))

        AppTextField(
            value = state.email,
            onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
            label = stringResource(Res.string.login_user_label),
            modifier = Modifier.fillMaxWidth(),
            errorMessage = state.emailError?.let { AppError.Validation(it).asText() },
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(dimens.spaceL))

        AppTextField(
            value = state.password,
            onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
            label = stringResource(Res.string.login_password_label),
            modifier = Modifier.fillMaxWidth(),
            isPassword = true,
            errorMessage = state.passwordError?.let { AppError.Validation(it).asText() },
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onEvent(LoginEvent.Submit)
            }),
        )

        state.error?.let { error ->
            Spacer(Modifier.height(dimens.spaceM))
            ErrorText(error.asText(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(dimens.spaceXl))
        PrimaryButton(
            text = stringResource(Res.string.login_action),
            onClick = {
                focusManager.clearFocus()
                onEvent(LoginEvent.Submit)
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isLoading,
        )
        Spacer(Modifier.weight(1.3f))
        // Compensa la altura del botón de regresar para que el centro sea el de la pantalla
        Spacer(Modifier.height(dimens.iconButtonSize))
    }
}
