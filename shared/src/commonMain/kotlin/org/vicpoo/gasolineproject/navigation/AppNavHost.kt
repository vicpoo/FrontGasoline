package org.vicpoo.gasolineproject.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.vicpoo.gasolineproject.feature.auth.presentation.license.LicenseRoute
import org.vicpoo.gasolineproject.feature.auth.presentation.login.LoginRoute
import org.vicpoo.gasolineproject.feature.auth.presentation.welcome.WelcomeScreen
import org.vicpoo.gasolineproject.feature.profile.presentation.editname.EditNameRoute
import org.vicpoo.gasolineproject.feature.trips.presentation.detail.TripDetailRoute
import org.vicpoo.gasolineproject.feature.vehicles.presentation.detail.VehicleDetailRoute
import org.vicpoo.gasolineproject.feature.vehicles.presentation.form.VehicleFormRoute

@Composable
fun AppNavHost(
    startRoute: AppRoute,
    isLoggedIn: Boolean?,
    navController: NavHostController = rememberNavController(),
) {
    // Si la sesión se cierra (Salir o token vencido -> 401), regresa a la bienvenida
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false) {
            val current = navController.currentDestination?.route.orEmpty()
            val inAuthFlow = listOf("Welcome", "Login").any { current.endsWith(it) }
            if (!inAuthFlow) navController.navigateClearingBackStack(AppRoute.Welcome)
        }
    }

    NavHost(navController = navController, startDestination = startRoute) {

        composable<AppRoute.Welcome> {
            WelcomeScreen(onAccess = { navController.navigate(AppRoute.Login) })
        }

        composable<AppRoute.Login> {
            LoginRoute(
                onBack = { navController.popBackStack() },
                onGoToUploadLicense = { navController.navigateClearingBackStack(AppRoute.UploadLicense) },
                onGoToHome = { navController.navigateClearingBackStack(AppRoute.Home) },
            )
        }

        composable<AppRoute.UploadLicense> {
            LicenseRoute(onGoToHome = { navController.navigateClearingBackStack(AppRoute.Home) })
        }

        composable<AppRoute.Home> {
            MainScreen(
                onVehicleClick = { id -> navController.navigate(AppRoute.VehicleDetail(id)) },
                onAddVehicle = { navController.navigate(AppRoute.VehicleForm()) },
                onTripClick = { id -> navController.navigate(AppRoute.TripDetail(id)) },
                onEditName = { navController.navigate(AppRoute.EditName) },
                onUpdateLicense = { navController.navigate(AppRoute.UpdateLicense) },
                onLoggedOut = { navController.navigateClearingBackStack(AppRoute.Welcome) },
            )
        }

        composable<AppRoute.TripDetail> { entry ->
            val route = entry.toRoute<AppRoute.TripDetail>()
            TripDetailRoute(tripId = route.tripId, onBack = { navController.popBackStack() })
        }

        composable<AppRoute.EditName> {
            EditNameRoute(onBack = { navController.popBackStack() })
        }

        composable<AppRoute.UpdateLicense> {
            LicenseRoute(
                isUpdate = true,
                onBack = { navController.popBackStack() },
                onGoToHome = { navController.popBackStack() },
            )
        }

        composable<AppRoute.VehicleDetail> { entry ->
            val route = entry.toRoute<AppRoute.VehicleDetail>()
            VehicleDetailRoute(
                vehicleId = route.vehicleId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(AppRoute.VehicleForm(id)) },
            )
        }

        composable<AppRoute.VehicleForm> { entry ->
            val route = entry.toRoute<AppRoute.VehicleForm>()
            VehicleFormRoute(
                vehicleId = route.vehicleId,
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}

/** Navega y borra el historial (para que "atrás" no regrese al login después de entrar). */
private fun NavHostController.navigateClearingBackStack(route: AppRoute) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
