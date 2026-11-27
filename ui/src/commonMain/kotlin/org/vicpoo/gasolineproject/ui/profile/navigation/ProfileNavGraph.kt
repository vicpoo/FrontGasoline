package org.vicpoo.gasolineproject.ui.profile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.ui.common.navigation.NavResults
import org.vicpoo.gasolineproject.ui.common.navigation.privilegedComposable
import org.vicpoo.gasolineproject.domain.rbac.AppAccess
import org.vicpoo.gasolineproject.ui.profile.ProfileRoute
import org.vicpoo.gasolineproject.ui.profile.editname.EditNameRoute

/** Subgrafo de la pestaña "Perfil" (todo usuario tiene perfil: nombre y foto son autoservicio). */
@Serializable
data object ProfileGraph

sealed interface ProfileDestination {
    @Serializable
    data object Overview : ProfileDestination

    @Serializable
    data object EditName : ProfileDestination

    /** Solo para quien maneja (viaje.registrar). */
    @Serializable
    data object UpdateLicense : ProfileDestination
}

/**
 * @param onLoggedOut lo resuelve el NavHost raíz (regresar a la bienvenida).
 * @param onCloseTripToLogout "Salir" con un viaje abierto: la app abre "Registrar Viaje" en modo salir.
 * @param updateLicenseScreen pantalla para actualizar la licencia. Es de la feature auth; la app
 *   la pasa aquí para que Perfil no dependa de auth. Recibe "cerrar" (atrás o al terminar).
 */
fun NavGraphBuilder.profileGraph(
    navController: NavHostController,
    onLoggedOut: () -> Unit,
    onCloseTripToLogout: () -> Unit,
    updateLicenseScreen: @Composable (onClose: () -> Unit) -> Unit,
) {
    navigation<ProfileGraph>(startDestination = ProfileDestination.Overview) {

        composable<ProfileDestination.Overview> { entry ->
            // Resultado de "Registrar Viaje" en modo salir: el viaje ya se guardó, sigue el cierre de sesión
            val tripClosed by entry.savedStateHandle
                .getStateFlow(NavResults.TRIP_CLOSED_FOR_LOGOUT, false)
                .collectAsStateWithLifecycle()
            ProfileRoute(
                onEditName = { navController.navigate(ProfileDestination.EditName) },
                onUpdateLicense = { navController.navigate(ProfileDestination.UpdateLicense) },
                onLoggedOut = onLoggedOut,
                onCloseTripToLogout = onCloseTripToLogout,
                tripClosedForLogout = tripClosed,
                onTripClosedHandled = { entry.savedStateHandle[NavResults.TRIP_CLOSED_FOR_LOGOUT] = false },
            )
        }

        composable<ProfileDestination.EditName> {
            EditNameRoute(onBack = { navController.popBackStack() })
        }

        // Pantalla de licencia de la feature auth en modo "actualizar" (la pone la app)
        privilegedComposable<ProfileDestination.UpdateLicense>(
            access = AppAccess.UPDATE_LICENSE,
            onDenied = { navController.popBackStack() },
        ) {
            updateLicenseScreen { navController.popBackStack() }
        }
    }
}
