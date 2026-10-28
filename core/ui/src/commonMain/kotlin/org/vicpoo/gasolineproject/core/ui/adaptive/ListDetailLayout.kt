package org.vicpoo.gasolineproject.core.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.rbac.AccessRule
import org.vicpoo.gasolineproject.core.rbac.RequireAccess
import org.vicpoo.gasolineproject.core.ui.components.AccessDeniedView
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Ancho de la lista cuando se ve junto al detalle. */
private val LIST_PANE_WIDTH = 380.dp

/**
 * Lista + detalle adaptativo:
 * - Pantalla grande ([showsListAndDetail]): la lista a la izquierda y el detalle de [selectedId]
 *   a la derecha (o [emptyDetailText] si no hay nada elegido).
 * - Teléfono / tablet vertical: solo la lista; tocar un elemento navega al detalle como siempre.
 *   Si la pantalla se achica con algo abierto (ej. se gira la tablet), se abre como pantalla
 *   aparte con [onOpenSinglePane] para no perder lo que el usuario estaba viendo.
 *
 * Quien lo usa guarda [selectedId] (rememberSaveable) y decide en el clic de la lista:
 * `if (showsListAndDetail) seleccionar else navegar`.
 *
 * @param detailAccess RBAC del detalle: en un panel no pasa por privilegedComposable, así que se
 *   revisa aquí con la misma regla que tiene la ruta del detalle.
 */
@Composable
fun ListDetailLayout(
    selectedId: String?,
    emptyDetailText: String,
    onOpenSinglePane: (String) -> Unit,
    onCloseDetail: () -> Unit,
    list: @Composable () -> Unit,
    detail: @Composable (id: String) -> Unit,
    detailAccess: AccessRule? = null,
) {
    if (!showsListAndDetail) {
        LaunchedEffect(selectedId) { selectedId?.let(onOpenSinglePane) }
        list()
        return
    }

    val colors = AppTheme.colors
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(LIST_PANE_WIDTH).fillMaxHeight()) {
            // Cada panel calcula su propio tamaño: la lista se dibuja como en teléfono
            ProvideWindowWidth { list() }
        }
        VerticalDivider(thickness = 1.dp, color = colors.textSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.25f))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            ProvideWindowWidth {
                if (selectedId == null) {
                    EmptyView(emptyDetailText)
                } else {
                    // key: al elegir otro elemento se descarta el estado del detalle anterior
                    key(selectedId) {
                        if (detailAccess == null) {
                            detail(selectedId)
                        } else {
                            RequireAccess(
                                rule = detailAccess,
                                fallback = { AccessDeniedView(onBack = onCloseDetail) },
                            ) { detail(selectedId) }
                        }
                    }
                }
            }
        }
    }
}
