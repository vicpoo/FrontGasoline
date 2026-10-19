package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/**
 * Formulario por pasos estilo Airbnb:
 * - Arriba: botón regresar/cerrar, nombre del flujo y "Paso X de Y".
 * - Centro: UNA pregunta por pantalla (título grande + ayuda + pocos campos), con animación entre pasos.
 * - Abajo: barra de progreso segmentada + "Atrás" (texto) y botón principal (Siguiente / Guardar).
 *
 * Reutilizable para cualquier formulario largo (vehículos, cargas de combustible, etc.).
 */
@Composable
fun WizardScaffold(
    flowTitle: String,
    stepLabel: String,
    currentStep: Int,
    totalSteps: Int,
    backText: String,
    nextText: String,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    isNextLoading: Boolean = false,
    isBackEnabled: Boolean = currentStep > 0,
    content: @Composable ColumnScope.(step: Int) -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
    ) {
        // ---- Encabezado ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding, vertical = dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppBackButton(onClick = onClose)
            Text(
                text = flowTitle,
                modifier = Modifier.weight(1f).padding(horizontal = dimens.spaceM),
                style = AppTheme.typography.bodySecondary,
                color = colors.textSecondary,
            )
            Text(text = stepLabel, style = AppTheme.typography.caption, color = colors.textSecondary)
        }

        // ---- Contenido del paso (animado) ----
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedContent(
                targetState = currentStep,
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { width -> if (forward) width / 3 else -width / 3 } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> if (forward) -width / 3 else width / 3 } + fadeOut())
                },
                label = "wizard-step",
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = dimens.maxContentWidth)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceM),
                ) {
                    content(step)
                }
            }
        }

        // ---- Pie: progreso + navegación ----
        StepProgressBar(currentStep = currentStep, totalSteps = totalSteps, modifier = Modifier.fillMaxWidth())
        HorizontalDivider(color = colors.divider)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding, vertical = dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextButton(text = backText, onClick = onBack, enabled = isBackEnabled && !isNextLoading)
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = nextText,
                onClick = onNext,
                isLoading = isNextLoading,
                modifier = Modifier.widthIn(min = 140.dp),
            )
        }
    }
}

/** Barra de progreso segmentada (un segmento por paso). */
@Composable
fun StepProgressBar(currentStep: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.padding(horizontal = AppTheme.dimens.screenPadding, vertical = AppTheme.dimens.spaceS),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(totalSteps) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        color = if (index <= currentStep) colors.primary else colors.labelBackground,
                        shape = RoundedCornerShape(50),
                    ),
            )
        }
    }
}

/** Encabezado de cada paso: pregunta grande + texto de ayuda. */
@Composable
fun WizardStepHeader(title: String, subtitle: String) {
    TitleText(title)
    Spacer(Modifier.height(AppTheme.dimens.spaceS))
    SecondaryText(subtitle)
    Spacer(Modifier.height(AppTheme.dimens.spaceXl))
}
