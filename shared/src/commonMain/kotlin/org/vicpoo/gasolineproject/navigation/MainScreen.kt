package org.vicpoo.gasolineproject.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.ui.adaptive.AdaptiveNavScaffold
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.feature.profile.presentation.navigation.profileGraph
import org.vicpoo.gasolineproject.feature.reservations.presentation.navigation.companyReservationsGraph
import org.vicpoo.gasolineproject.feature.triprecording.presentation.navigation.tripRecordingGraph
import org.vicpoo.gasolineproject.feature.trips.presentation.navigation.tripHistoryGraph
import org.vicpoo.gasolineproject.feature.vehicles.presentation.navigation.catalogGraph
import org.vicpoo.gasolineproject.feature.vehicles.presentation.navigation.vehiclesGraph
import org.vicpoo.gasolineproject.core.ui.adaptive.NavDestination as TabItem

/**
 * Pantalla principal con NAVEGACIÓN ANIDADA:
 * - Tiene su propio NavHost (independiente del raíz) con un subgrafo por pestaña.
 * - Cada pestaña conserva su historial al cambiar de pestaña (saveState / restoreState).
 * - La barra (o el riel en tablet) solo se ve en la pantalla inicial de cada subgrafo;
 *   los detalles y formularios se abren a pantalla completa.
 */
@Composable
fun MainScreen(
    onLoggedOut: () -> Unit,
    viewModel: MainViewModel = koinViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val privileges by viewModel.privileges.collectAsStateWithLifecycle()
    val granted = privileges ?: run {
        LoadingView()
        return
    }

    val tabs = TopLevelDestination.entries.filter { it.isAllowed(granted) }
    // El grafo de inicio se fija una vez (cambiarlo reiniciaría la navegación)
    val startGraph = remember { tabs.first().graph }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTab = tabs.firstOrNull { currentDestination.belongsTo(it) }
    val onTopLevel = currentDestination == null || tabs.any { currentDestination.hasRoute(it.start::class) }

    AdaptiveNavScaffold(
        destinations = tabs.map { TabItem(it.name, stringResource(it.label), it.icon()) },
        selectedKey = (currentTab ?: tabs.first()).name,
        onSelect = { key -> navController.navigateToTab(TopLevelDestination.valueOf(key), currentTab) },
        visible = onTopLevel,
    ) {
        NavHost(navController = navController, startDestination = startGraph) {
            catalogGraph(navController)
            tripRecordingGraph(navController)
            vehiclesGraph(navController)
            companyReservationsGraph(navController)
            tripHistoryGraph(navController)
            profileGraph(navController, onLoggedOut = onLoggedOut)
        }
    }
}

private fun NavDestination?.belongsTo(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.graph::class) } == true

/**
 * Patrón recomendado para pestañas con subgrafos:
 * - Otra pestaña: guarda el historial de la actual y restaura el de la nueva.
 * - La misma pestaña: regresa a su pantalla inicial.
 */
private fun NavHostController.navigateToTab(tab: TopLevelDestination, current: TopLevelDestination?) {
    if (tab == current) {
        popBackStack(route = tab.start, inclusive = false)
        return
    }
    navigate(tab.graph) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
