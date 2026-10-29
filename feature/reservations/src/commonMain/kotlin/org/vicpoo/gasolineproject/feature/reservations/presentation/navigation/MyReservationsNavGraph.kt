package org.vicpoo.gasolineproject.feature.reservations.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.navigation.privilegedComposable
import org.vicpoo.gasolineproject.core.rbac.AppAccess
import org.vicpoo.gasolineproject.feature.reservations.presentation.detail.ReservationDetailRoute
import org.vicpoo.gasolineproject.feature.reservations.presentation.list.MyReservationsRoute

/** "Mis solicitudes" (lo que abre la campana): el estado de las solicitudes del empleado. */
@Serializable
data object MyReservationsDestination

/** Detalle de una solicitud propia (solo consulta: estado, carro, licencia, quién respondió y el motivo del rechazo). */
@Serializable
data class MyReservationDetailDestination(val reservationId: String)

/**
 * Registra "Mis solicitudes" y su detalle dentro del subgrafo que la app indique (hoy: la pestaña Inicio).
 * La feature de reservas es dueña de sus pantallas; quien la contiene no necesita conocerlas.
 */
fun NavGraphBuilder.myReservationsDestination(navController: NavHostController) {
    privilegedComposable<MyReservationsDestination>(
        access = AppAccess.MY_RESERVATIONS,
        onDenied = { navController.popBackStack() },
    ) {
        MyReservationsRoute(
            onBack = { navController.popBackStack() },
            onRequestClick = { id -> navController.navigate(MyReservationDetailDestination(id)) },
        )
    }

    privilegedComposable<MyReservationDetailDestination>(
        access = AppAccess.MY_RESERVATIONS,
        onDenied = { navController.popBackStack() },
    ) { entry ->
        val route = entry.toRoute<MyReservationDetailDestination>()
        ReservationDetailRoute(
            reservationId = route.reservationId,
            onBack = { navController.popBackStack() },
            // Desde la campana solo se consulta: aceptar / rechazar es de la pestaña Solicitudes
            allowDecision = false,
        )
    }
}
