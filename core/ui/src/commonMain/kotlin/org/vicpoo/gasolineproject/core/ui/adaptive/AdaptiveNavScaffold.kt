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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 *
 * Estilo Airbnb: la barra tiene el mismo fondo que la pantalla y se separa del contenido con
 * una línea fina arriba (visible en claro y en oscuro) más una sombra suave en modo claro.
 * El ícono activo va en el color principal y los demás en gris.
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
    val separator = colors.textSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.25f)
    val shadow = if (colors.isDark) 0.dp else 16.dp

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
                Surface(color = colors.background, shadowElevation = shadow) {
                    Column {
                        HorizontalDivider(thickness = 1.dp, color = separator)
                        NavigationBar(containerColor = colors.background, contentColor = colors.text, tonalElevation = 0.dp) {
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
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = colors.textSecondary,
                                        unselectedTextColor = colors.textSecondary,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            if (showNavigation) {
                Surface(color = colors.background, shadowElevation = shadow, modifier = Modifier.fillMaxHeight()) {
                    Row {
                        NavigationRail(containerColor = colors.background, contentColor = colors.text, modifier = Modifier.fillMaxHeight()) {
                            destinations.forEach { destination ->
                                NavigationRailItem(
                                    selected = destination.key == selectedKey,
                                    onClick = { onSelect(destination.key) },
                                    icon = { Icon(destination.icon, contentDescription = destination.label, modifier = Modifier.size(26.dp)) },
                                    alwaysShowLabel = false,
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = colors.primary,
                                        selectedTextColor = colors.primary,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = colors.textSecondary,
                                        unselectedTextColor = colors.textSecondary,
                                    ),
                                )
                            }
                        }
                        VerticalDivider(thickness = 1.dp, color = separator)
                    }
                }
            }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}
