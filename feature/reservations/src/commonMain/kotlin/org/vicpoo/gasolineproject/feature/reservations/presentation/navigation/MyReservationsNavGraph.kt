package org.vicpoo.gasolineproject.feature.reservations.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.navigation.privilegedComposable
import org.vicpoo.gasolineproject.core.rbac.AppAccess
import org.vicpoo.gasolineproject.feature.reservations.presentation.list.MyReservationsRoute

/** "Mis solicitudes" (lo que abre la campana): el estado de las solicitudes del empleado. */
@Serializable
data object MyReservationsDestination

/**
 * Registra "Mis solicitudes" dentro del subgrafo que la app indique (hoy: la pestaña Inicio).
 * La feature de reservas es dueña de su pantalla; quien la contiene no necesita conocerla.
 */
fun NavGraphBuilder.myReservationsDestination(navController: NavHostController) {
    privilegedComposable<MyReservationsDestination>(
        access = AppAccess.MY_RESERVATIONS,
        onDenied = { navController.popBackStack() },
    ) {
        MyReservationsRoute(onBack = { navController.popBackStack() })
    }
}
