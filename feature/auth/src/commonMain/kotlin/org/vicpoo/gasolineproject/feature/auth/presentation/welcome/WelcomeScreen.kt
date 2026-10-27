package org.vicpoo.gasolineproject.feature.auth.presentation.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.adaptive.LocalWindowWidth
import org.vicpoo.gasolineproject.core.ui.adaptive.WindowWidth
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.theme.AppImages
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.auth.resources.Res
import org.vicpoo.gasolineproject.feature.auth.resources.action_access
import org.vicpoo.gasolineproject.feature.auth.resources.welcome_subtitle
import org.vicpoo.gasolineproject.feature.auth.resources.welcome_title

/** Pantalla de bienvenida (Figma: "Control total de tus vehículos"). Sin estado: no necesita ViewModel. */
@Composable
fun WelcomeScreen(onAccess: () -> Unit) {
    if (LocalWindowWidth.current == WindowWidth.Expanded) {
        WelcomeExpanded(onAccess)
    } else {
        WelcomeCompact(onAccess)
    }
}

/** Teléfono / tablet vertical: todo en columna, botón abajo. */
@Composable
private fun WelcomeCompact(onAccess: () -> Unit) {
    val dimens = AppTheme.dimens
    AppScreen {
        Spacer(Modifier.height(dimens.spaceXxl))
        CarIllustration(Modifier.fillMaxWidth())
        Spacer(Modifier.height(dimens.spaceL))
        WelcomeTexts()
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(dimens.spaceL))
        PrimaryButton(
            text = stringResource(Res.string.action_access),
            onClick = onAccess,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Tablet horizontal: ilustración a la izquierda, textos y botón a la derecha. */
@Composable
private fun WelcomeExpanded(onAccess: () -> Unit) {
    val dimens = AppTheme.dimens
    AppScreen(maxContentWidth = dimens.maxContentWidthWide, scrollable = false) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CarIllustration(Modifier.weight(1f))
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = dimens.spaceXl),
                verticalArrangement = Arrangement.Center,
            ) {
                WelcomeTexts()
                Spacer(Modifier.height(dimens.spaceXl))
                PrimaryButton(
                    text = stringResource(Res.string.action_access),
                    onClick = onAccess,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CarIllustration(modifier: Modifier = Modifier) {
    Image(
        // Cambia sola entre la versión clara y la oscura según el tema del teléfono
        painter = painterResource(AppImages.carHero),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillWidth,
    )
}

@Composable
private fun WelcomeTexts() {
    TitleText(stringResource(Res.string.welcome_title))
    Spacer(Modifier.height(AppTheme.dimens.spaceM))
    SecondaryText(stringResource(Res.string.welcome_subtitle))
}
