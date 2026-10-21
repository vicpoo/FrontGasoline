package org.vicpoo.gasolineproject.feature.profile.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.navigation.privilegedComposable
import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.feature.auth.presentation.license.LicenseRoute
import org.vicpoo.gasolineproject.feature.profile.presentation.ProfileRoute
import org.vicpoo.gasolineproject.feature.profile.presentation.editname.EditNameRoute

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

/** [onLoggedOut] lo resuelve el NavHost raíz (regresar a la bienvenida). */
fun NavGraphBuilder.profileGraph(navController: NavHostController, onLoggedOut: () -> Unit) {
    navigation<ProfileGraph>(startDestination = ProfileDestination.Overview) {

        composable<ProfileDestination.Overview> {
            ProfileRoute(
                onEditName = { navController.navigate(ProfileDestination.EditName) },
                onUpdateLicense = { navController.navigate(ProfileDestination.UpdateLicense) },
                onLoggedOut = onLoggedOut,
            )
        }

        composable<ProfileDestination.EditName> {
            EditNameRoute(onBack = { navController.popBackStack() })
        }

        // Reutiliza la pantalla de licencia de la feature auth en modo "actualizar"
        privilegedComposable<ProfileDestination.UpdateLicense>(
            privileges = setOf(Privileges.VIAJE_REGISTRAR),
            onDenied = { navController.popBackStack() },
        ) {
            LicenseRoute(
                isUpdate = true,
                onBack = { navController.popBackStack() },
                onGoToHome = { navController.popBackStack() },
            )
        }
    }
}
