package org.vicpoo.gasolineproject.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.push.PushEvents
import org.vicpoo.gasolineproject.core.push.PushTarget
import org.vicpoo.gasolineproject.core.push.PushType
import org.vicpoo.gasolineproject.core.push.RequestNotificationPermission
import org.vicpoo.gasolineproject.core.ui.adaptive.AdaptiveNavScaffold
import org.vicpoo.gasolineproject.core.ui.adaptive.NavDestination as TabItem
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.feature.auth.presentation.license.LicenseRoute
import org.vicpoo.gasolineproject.feature.auth.presentation.license.RequireLicense
import org.vicpoo.gasolineproject.feature.profile.presentation.navigation.profileGraph
import org.vicpoo.gasolineproject.feature.reservations.presentation.bell.ReservationsBell
import org.vicpoo.gasolineproject.feature.reservations.presentation.navigation.CompanyReservationsDestination
import org.vicpoo.gasolineproject.feature.reservations.presentation.navigation.MyReservationsDestination
import org.vicpoo.gasolineproject.feature.reservations.presentation.navigation.companyReservationsGraph
import org.vicpoo.gasolineproject.feature.reservations.presentation.navigation.myReservationsDestination
import org.vicpoo.gasolineproject.feature.triprecording.presentation.navigation.tripRecordingGraph
import org.vicpoo.gasolineproject.feature.trips.presentation.navigation.tripHistoryGraph
import org.vicpoo.gasolineproject.feature.vehicles.presentation.navigation.catalogGraph
import org.vicpoo.gasolineproject.feature.vehicles.presentation.navigation.vehiclesGraph

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
    pushEvents: PushEvents = koinInject(),
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

    // Notificaciones push: se pide el permiso y, si se tocó una, se abre su pantalla
    RequestNotificationPermission()
    val pushTarget by pushEvents.pendingOpen.collectAsStateWithLifecycle()
    LaunchedEffect(pushTarget) {
        val target = pushTarget ?: return@LaunchedEffect
        // Espera a que el NavHost ya tenga su grafo (la primera vez se arma después de este efecto)
        snapshotFlow { navController.currentBackStackEntry }.filterNotNull().first()
        navController.openPushTarget(target)
        pushEvents.consumeOpen()
    }

    AdaptiveNavScaffold(
        destinations = tabs.map { TabItem(it.name, stringResource(it.label), it.icon()) },
        selectedKey = (currentTab ?: tabs.first()).name,
        onSelect = { key -> navController.navigateToTab(TopLevelDestination.valueOf(key), currentTab) },
        visible = onTopLevel,
    ) {
        // Aquí (nivel app) se conectan las features entre sí: ninguna feature importa a otra.
        NavHost(navController = navController, startDestination = startGraph) {
            catalogGraph(
                navController = navController,
                // Campana de "Mis solicitudes" (reservas) en la lista de carros (vehículos)
                topEndAction = {
                    ReservationsBell(onClick = {
                        navController.navigate(MyReservationsDestination) { launchSingleTop = true }
                    })
                },
                extraDestinations = { myReservationsDestination(navController) },
            )
            tripRecordingGraph(
                navController = navController,
                // Regla y pantalla de licencia (auth) dentro de Registrar (triprecording)
                licenseGate = { onUploadLicense, content -> RequireLicense(onUploadLicense = onUploadLicense, content = content) },
                uploadLicenseScreen = { onClose -> UpdateLicenseRoute(onClose) },
            )
            vehiclesGraph(navController)
            companyReservationsGraph(navController)
            tripHistoryGraph(navController)
            profileGraph(
                navController = navController,
                onLoggedOut = onLoggedOut,
                updateLicenseScreen = { onClose -> UpdateLicenseRoute(onClose) },
            )
        }
    }
}

/** Pantalla de licencia de auth en modo "actualizar": atrás y al terminar solo se cierra. */
@Composable
private fun UpdateLicenseRoute(onClose: () -> Unit) {
    LicenseRoute(isUpdate = true, onBack = onClose, onGoToHome = onClose)
}

/**
 * Pantalla de cada push (sin el privilegio, privilegedComposable regresa solo):
 * - Nueva solicitud → su detalle en la pestaña Solicitudes, para aceptarla o rechazarla.
 * - Resultado de mi solicitud → "Mis solicitudes" (lo mismo que abre la campana).
 */
private fun NavHostController.openPushTarget(target: PushTarget) {
    when (target.type) {
        PushType.NEW_REQUEST -> target.referenceId?.let { id ->
            navigate(CompanyReservationsDestination.Detail(id)) { launchSingleTop = true }
        }
        PushType.REQUEST_RESULT -> navigate(MyReservationsDestination) { launchSingleTop = true }
        PushType.VEHICLE_STATUS, PushType.OTHER -> Unit
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
