package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Alto del buscador compacto (como Instagram: discreto, no compite con el contenido). */
private val SearchBarHeight = 40.dp

/** Fondo gris suave del buscador (en oscuro, blanco muy transparente). */
@Composable
private fun searchFill() = AppTheme.colors.let { if (it.isDark) it.text.copy(alpha = 0.10f) else it.labelBackground.copy(alpha = 0.6f) }

/**
 * Buscador estilo Instagram: barra baja y gris con la lupa a la izquierda.
 * NO es un campo de texto: al tocarlo NO abre el teclado aquí, sino [onClick] (que abre la vista
 * de búsqueda y filtros, como Instagram, Facebook o Airbnb).
 *
 * - [text]: lo que se buscó (vacío = se ve el [placeholder]).
 * - [activeFilters]: número de filtros aplicados; se muestra como insignia en el ícono de filtros.
 * - [onClear]: si se da y hay búsqueda o filtros, aparece la X para quitarlos de un toque.
 */
@Composable
fun AppSearchBar(
    text: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeFilters: Int = 0,
    onClear: (() -> Unit)? = null,
    clearDescription: String? = null,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val hasQuery = text.isNotBlank()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(SearchBarHeight),
        shape = RoundedCornerShape(dimens.radius),
        color = searchFill(),
    ) {
        Row(
            modifier = Modifier.padding(start = dimens.spaceM - dimens.spaceXs, end = dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(dimens.spaceS))
            Text(
                text = if (hasQuery) text else placeholder,
                style = AppTheme.typography.body,
                color = if (hasQuery) colors.text else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (activeFilters > 0) {
                FilterBadgeIcon(activeFilters)
            }
            if (onClear != null && (hasQuery || activeFilters > 0)) {
                Surface(onClick = onClear, shape = CircleShape, color = colors.background.copy(alpha = 0f)) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Close, contentDescription = clearDescription, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Spacer(Modifier.width(dimens.spaceS))
            }
        }
    }
}

/** Ícono de filtros con el número de filtros activos en una bolita. */
@Composable
private fun FilterBadgeIcon(count: Int) {
    val colors = AppTheme.colors
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Icon(AppIcons.Filter, contentDescription = null, tint = colors.title, modifier = Modifier.size(20.dp))
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(16.dp)
                .background(colors.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                style = AppTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onPrimary,
                maxLines = 1,
            )
        }
    }
}

/**
 * Vista completa de búsqueda (la que abre [AppSearchBar]), estilo Instagram / Airbnb:
 * arriba "‹" + campo con el teclado ya abierto; en medio [content] (filtros y resultados, con
 * scroll propio); abajo [bottomBar] fijo (ej. "Borrar todo" / "Ver 12 vehículos").
 *
 * Se muestra en un diálogo de pantalla completa para tapar también la barra de pestañas.
 */
@Composable
fun FullScreenSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    clearDescription: String? = null,
    autoFocus: Boolean = true,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .safeDrawingPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.fillMaxSize().widthIn(max = dimens.maxContentWidthWide)) {
                // "‹" + campo de búsqueda
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding, vertical = dimens.spaceS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppBackButton(onClick = onDismiss)
                    Spacer(Modifier.width(dimens.spaceS))
                    SearchInput(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = placeholder,
                        clearDescription = clearDescription,
                        onSearch = {
                            focusManager.clearFocus()
                            onSearch()
                        },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    )
                }
                Column(Modifier.weight(1f).fillMaxWidth(), content = content)
                if (bottomBar != null) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    Box(Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding, vertical = dimens.spaceM)) {
                        bottomBar()
                    }
                }
            }
        }

        // Aquí sí se abre el teclado: es la vista dedicada a buscar
        LaunchedEffect(Unit) {
            if (autoFocus) runCatching { focusRequester.requestFocus() }
        }
    }
}

/** Campo de la vista de búsqueda: mismo look que [AppSearchBar] pero editable. */
@Composable
private fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String?,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = AppTheme.typography.body.copy(color = colors.text),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier.height(SearchBarHeight),
        decorationBox = { innerField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(searchFill(), RoundedCornerShape(dimens.radius))
                    .padding(start = dimens.spaceM - dimens.spaceXs, end = dimens.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(dimens.spaceS))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = AppTheme.typography.body, color = colors.textSecondary, maxLines = 1)
                    }
                    innerField()
                }
                if (value.isNotEmpty()) {
                    Surface(onClick = { onValueChange("") }, shape = CircleShape, color = colors.background.copy(alpha = 0f)) {
                        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            Icon(AppIcons.Close, contentDescription = clearDescription, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Spacer(Modifier.width(dimens.spaceS))
                }
            }
        },
    )
}

/**
 * Píldora seleccionable para filtros (Estado, Tipo...). Seleccionada: fondo del color principal
 * suave y borde del color principal; sin seleccionar: solo borde gris.
 */
@Composable
fun AppFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(50),
        color = if (selected) colors.primary.copy(alpha = if (colors.isDark) 0.25f else 0.10f) else colors.background,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) colors.primary else colors.textSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.25f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimens.spaceM - dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tint = if (selected) colors.primary else colors.title
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = AppTheme.typography.bodySecondary.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                color = if (selected) colors.primary else colors.title,
                maxLines = 1,
            )
        }
    }
}
