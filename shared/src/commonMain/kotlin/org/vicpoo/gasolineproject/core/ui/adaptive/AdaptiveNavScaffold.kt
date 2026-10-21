package org.vicpoo.gasolineproject.core.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/** Destino de la navegación principal. */
data class NavDestination(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Navegación principal adaptativa:
 * - Teléfono (Compact): barra inferior solo con íconos (Figma).
 * - Tablet (Medium / Expanded): riel lateral.
 * Los destinos que recibe ya vienen filtrados por privilegios (RBAC).
 */
@Composable
fun AdaptiveNavScaffold(
    destinations: List<NavDestination>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    /** false en pantallas de detalle / formularios (se ven a pantalla completa). */
    visible: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    val showNavigation = visible && destinations.size > 1

    if (LocalWindowWidth.current == WindowWidth.Compact) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .then(
                        if (showNavigation) {
                            Modifier.consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        } else {
                            Modifier
                        },
                    ),
            ) { content() }
            if (showNavigation) {
                NavigationBar(containerColor = colors.background) {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = destination.key == selectedKey,
                            onClick = { onSelect(destination.key) },
                            // Solo ícono (Figma); el nombre queda para accesibilidad
                            icon = { Icon(destination.icon, contentDescription = destination.label, modifier = Modifier.size(26.dp)) },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                indicatorColor = colors.labelBackground,
                                unselectedIconColor = colors.text,
                                unselectedTextColor = colors.text,
                            ),
                        )
                    }
                }
            }
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            if (showNavigation) {
                NavigationRail(containerColor = colors.background, modifier = Modifier.fillMaxHeight()) {
                    destinations.forEach { destination ->
                        NavigationRailItem(
                            selected = destination.key == selectedKey,
                            onClick = { onSelect(destination.key) },
                            icon = { Icon(destination.icon, contentDescription = destination.label, modifier = Modifier.size(26.dp)) },
                            alwaysShowLabel = false,
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                indicatorColor = colors.labelBackground,
                                unselectedIconColor = colors.text,
                                unselectedTextColor = colors.text,
                            ),
                        )
                    }
                }
            }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}
