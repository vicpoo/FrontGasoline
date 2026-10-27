package org.vicpoo.gasolineproject.feature.reservations.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.navigation.privilegedComposable
import org.vicpoo.gasolineproject.core.rbac.AppAccess
import org.vicpoo.gasolineproject.feature.reservations.presentation.company.CompanyReservationsRoute
import org.vicpoo.gasolineproject.feature.reservations.presentation.detail.ReservationDetailRoute

/** Subgrafo de la pestaña llave: solicitudes de la empresa y su detalle. */
@Serializable
data object CompanyReservationsGraph

sealed interface CompanyReservationsDestination {
    @Serializable
    data object List : CompanyReservationsDestination

    /** Detalle: motivo, solicitante (foto y licencia), carro y Aceptar / Rechazar. */
    @Serializable
    data class Detail(val reservationId: String) : CompanyReservationsDestination
}

fun NavGraphBuilder.companyReservationsGraph(navController: NavHostController) {
    navigation<CompanyReservationsGraph>(startDestination = CompanyReservationsDestination.List) {
        privilegedComposable<CompanyReservationsDestination.List>(AppAccess.COMPANY_REQUESTS) {
            CompanyReservationsRoute(
                onRequestClick = { id -> navController.navigate(CompanyReservationsDestination.Detail(id)) },
            )
        }

        privilegedComposable<CompanyReservationsDestination.Detail>(
            // Lista y detalle son la misma función (ver las solicitudes de la empresa)
            access = AppAccess.COMPANY_REQUESTS,
            onDenied = { navController.popBackStack() },
        ) { entry ->
            val route = entry.toRoute<CompanyReservationsDestination.Detail>()
            ReservationDetailRoute(
                reservationId = route.reservationId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
