package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.error_forbidden
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Pantalla para un destino al que el usuario ya no tiene acceso (RBAC en la navegación). */
@Composable
fun AccessDeniedView(onBack: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding()
            .padding(AppTheme.dimens.screenPadding),
    ) {
        if (onBack != null) AppBackButton(onClick = onBack)
        EmptyView(stringResource(Res.string.error_forbidden))
    }
}
