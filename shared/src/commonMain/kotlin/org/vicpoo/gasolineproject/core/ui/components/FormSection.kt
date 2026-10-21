package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

// =====================================================================================
// Formulario estilo Airbnb ("Evaluar y continuar"), componentes reutilizables:
// - ReviewCard: tarjeta con borde que agrupa las secciones.
// - ReviewRow: "Título / valor" con el botón gris "Agregar" o "Modificar".
// - SegmentedProgress: barra de avance en segmentos (arriba del botón principal).
// - EditSheet: hoja que sube desde abajo con título grande, X, "Cancelar" y "Guardar".
// =====================================================================================

/** Tarjeta con borde redondeado (contiene filas ReviewRow). */
@Composable
fun ReviewCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppTheme.dimens.radiusM * 2),
        color = AppTheme.colors.background,
        border = BorderStroke(1.dp, AppTheme.colors.divider),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.spaceL, vertical = AppTheme.dimens.spaceS)) {
            content()
        }
    }
}

/**
 * Fila de la tarjeta: título en negritas, el valor capturado debajo y el botón gris a la derecha.
 * Con [isError] el valor se muestra en rojo (ej. "Revisa los datos").
 */
@Composable
fun ReviewRow(
    title: String,
    value: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    showDivider: Boolean = true,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceM),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.title)
                Spacer(Modifier.height(dimens.spaceXs))
                Text(value, style = AppTheme.typography.body, color = if (isError) colors.error else colors.textSecondary)
            }
            Spacer(Modifier.width(dimens.spaceM))
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(dimens.radiusM),
                color = colors.labelBackground,
                contentColor = colors.onLabel,
            ) {
                Text(
                    text = actionText,
                    style = AppTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(horizontal = dimens.spaceM, vertical = dimens.spaceS),
                )
            }
        }
        if (showDivider) HorizontalDivider(color = colors.divider)
    }
}

/** Barra de avance en segmentos (Airbnb): [done] de [total] llenos. */
@Composable
fun SegmentedProgress(total: Int, done: Int, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(modifier.fillMaxWidth().height(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            val fill by animateFloatAsState(if (index < done) 1f else 0f)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.divider),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(colors.primary))
            }
        }
    }
}

/**
 * Hoja que sube desde abajo para editar una sección (Airbnb "Modifica el número de huéspedes"):
 * X arriba a la derecha, título grande, contenido con scroll y abajo "Cancelar" (texto subrayado)
 * y "Guardar" (botón). Ocupa casi toda la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheet(
    title: String,
    cancelText: String,
    saveText: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isSaving: Boolean = false,
    closeDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onCancel() },
        sheetState = sheetState,
        modifier = modifier,
        containerColor = colors.background,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // X para cerrar
            Row(Modifier.fillMaxWidth().padding(top = dimens.spaceS, end = dimens.spaceS), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { if (!isSaving) onCancel() }) {
                    Icon(AppIcons.Close, contentDescription = closeDescription, tint = colors.title)
                }
            }
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.spaceL),
            ) {
                TitleText(title)
                subtitle?.let {
                    Spacer(Modifier.height(dimens.spaceM))
                    SecondaryText(it)
                }
                Spacer(Modifier.height(dimens.spaceL))
                content()
                Spacer(Modifier.height(dimens.spaceL))
            }
            HorizontalDivider(color = colors.divider)
            // Cancelar (texto) · Guardar (botón)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = dimens.spaceL, vertical = dimens.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = cancelText,
                    style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
                    color = colors.title,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dimens.radiusS))
                        .clickable(enabled = !isSaving, onClick = onCancel)
                        .padding(vertical = dimens.spaceS),
                )
                PrimaryButton(
                    text = saveText,
                    onClick = onSave,
                    isLoading = isSaving,
                    modifier = Modifier.heightIn(min = 48.dp).width(140.dp),
                )
            }
        }
    }
}
