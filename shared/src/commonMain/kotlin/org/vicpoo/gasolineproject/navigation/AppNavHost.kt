package org.vicpoo.gasolineproject.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.vicpoo.gasolineproject.core.navigation.navigateClearingBackStack
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.AuthStatus

/**
 * NavHost raíz: solo decide entre el subgrafo de autenticación y la pantalla principal.
 * Las pantallas de cada pestaña viven en el NavHost anidado de MainScreen.
 */
@Composable
fun AppNavHost(
    startStatus: AuthStatus,
    isLoggedIn: Boolean?,
    navController: NavHostController = rememberNavController(),
) {
    // Si la sesión se cierra (Salir o token vencido -> 401), regresa a la bienvenida
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false && !navController.isInAuthGraph()) {
            navController.navigateClearingBackStack(AuthDestination.Welcome)
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (startStatus == AuthStatus.LoggedIn) HomeRoute else AuthGraph,
    ) {
        authGraph(
            navController = navController,
            startDestination = if (startStatus == AuthStatus.NeedsLicense) {
                AuthDestination.UploadLicense
            } else {
                AuthDestination.Welcome
            },
        )

        composable<HomeRoute> {
            MainScreen(onLoggedOut = { navController.navigateClearingBackStack(AuthDestination.Welcome) })
        }
    }
}

/** null (el grafo aún no se arma) cuenta como "ya está en autenticación": no hay a dónde regresar. */
private fun NavHostController.isInAuthGraph(): Boolean =
    currentBackStackEntry?.destination?.hierarchy?.any { it.hasRoute(AuthGraph::class) } != false
