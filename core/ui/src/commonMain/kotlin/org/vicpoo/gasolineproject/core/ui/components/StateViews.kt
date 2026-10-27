package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.resources.Res
import org.vicpoo.gasolineproject.core.ui.resources.action_retry
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Pantalla completa de carga. */
@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(AppTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AppTheme.colors.primary)
    }
}

/** Pantalla completa de error con botón Reintentar (rojo, según la guía). */
@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = AppTheme.dimens
    Column(
        modifier = modifier.fillMaxSize().background(AppTheme.colors.background).padding(dimens.screenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BodyText(message, textAlign = TextAlign.Center)
        Spacer(Modifier.height(dimens.spaceL))
        DangerButton(text = stringResource(Res.string.action_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth())
    }
}

/** Estado vacío (listas sin datos). */
@Composable
fun EmptyView(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(AppTheme.colors.background).padding(AppTheme.dimens.screenPadding),
        contentAlignment = Alignment.Center,
    ) {
        SecondaryText(message, textAlign = TextAlign.Center)
    }
}
