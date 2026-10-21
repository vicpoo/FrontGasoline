package org.vicpoo.gasolineproject.feature.auth.presentation.license

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.license_gate_button
import gasolineproject.shared.generated.resources.license_gate_message
import gasolineproject.shared.generated.resources.license_gate_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.SubtitleText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.ObserveMustUploadLicenseUseCase

/**
 * Pestaña de viajes: solo se entra con la licencia subida. Si al usuario le dieron
 * viaje.registrar después del login y todavía no la sube, se le pide aquí (no se le bloquea la app).
 */
@Composable
fun RequireLicense(
    onUploadLicense: () -> Unit,
    content: @Composable () -> Unit,
) {
    val observe = koinInject<ObserveMustUploadLicenseUseCase>()
    val flow = remember(observe) { observe() }
    val mustUpload by flow.collectAsStateWithLifecycle(initialValue = false)
    if (mustUpload) LicenseRequiredView(onUploadLicense) else content()
}

@Composable
private fun LicenseRequiredView(onUploadLicense: () -> Unit) {
    val dimens = AppTheme.dimens
    Column(
        modifier = Modifier.fillMaxSize().padding(dimens.screenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = dimens.maxContentWidth), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(AppIcons.FileDocument, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(dimens.spaceM))
            SubtitleText(stringResource(Res.string.license_gate_title), textAlign = TextAlign.Center)
            Spacer(Modifier.height(dimens.spaceS))
            SecondaryText(stringResource(Res.string.license_gate_message), textAlign = TextAlign.Center)
            Spacer(Modifier.height(dimens.spaceL))
            PrimaryButton(
                text = stringResource(Res.string.license_gate_button),
                onClick = onUploadLicense,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
