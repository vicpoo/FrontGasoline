package org.vicpoo.gasolineproject.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import org.vicpoo.gasolineproject.core.navigation.navigateClearingBackStack
import org.vicpoo.gasolineproject.feature.auth.presentation.license.LicenseRoute
import org.vicpoo.gasolineproject.feature.auth.presentation.login.LoginRoute
import org.vicpoo.gasolineproject.feature.auth.presentation.welcome.WelcomeScreen

/**
 * Subgrafo de autenticación.
 * [startDestination] = UploadLicense cuando la app se abre con sesión pero sin licencia.
 */
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    startDestination: AuthDestination,
) {
    navigation<AuthGraph>(startDestination = startDestination) {

        // Desactivada (ver SHOW_WELCOME_SCREEN): la ruta se conserva por si se vuelve a usar
        composable<AuthDestination.Welcome> {
            WelcomeScreen(onAccess = { navController.navigate(AuthDestination.Login) })
        }

        composable<AuthDestination.Login> {
            LoginRoute(
                // Sin bienvenida, el login es la primera pantalla: no hay a dónde regresar
                onBack = if (SHOW_WELCOME_SCREEN) {
                    { navController.popBackStack() }
                } else {
                    null
                },
                onGoToUploadLicense = { navController.navigateClearingBackStack(AuthDestination.UploadLicense) },
                onGoToHome = { navController.navigateClearingBackStack(HomeRoute) },
            )
        }

        composable<AuthDestination.UploadLicense> {
            LicenseRoute(onGoToHome = { navController.navigateClearingBackStack(HomeRoute) })
        }
    }
}
