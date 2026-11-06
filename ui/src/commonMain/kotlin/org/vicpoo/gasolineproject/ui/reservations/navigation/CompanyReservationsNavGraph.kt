package org.vicpoo.gasolineproject.ui.reservations.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.ui.common.navigation.privilegedComposable
import org.vicpoo.gasolineproject.domain.rbac.AppAccess
import org.vicpoo.gasolineproject.ui.common.adaptive.ListDetailLayout
import org.vicpoo.gasolineproject.ui.common.adaptive.showsListAndDetail
import org.vicpoo.gasolineproject.ui.reservations.company.CompanyReservationsRoute
import org.vicpoo.gasolineproject.ui.reservations.detail.ReservationDetailRoute
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.requests_select_hint

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
            // Tablet horizontal: lista y detalle (aceptar / rechazar) lado a lado
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            val twoPanes = showsListAndDetail
            ListDetailLayout(
                selectedId = selected,
                emptyDetailText = stringResource(Res.string.requests_select_hint),
                detailAccess = AppAccess.COMPANY_REQUESTS,
                onOpenSinglePane = { id ->
                    selected = null
                    navController.navigate(CompanyReservationsDestination.Detail(id))
                },
                onCloseDetail = { selected = null },
                list = {
                    CompanyReservationsRoute(
                        onRequestClick = { id ->
                            if (twoPanes) selected = id else navController.navigate(CompanyReservationsDestination.Detail(id))
                        },
                    )
                },
                // Al aceptar o rechazar, el detalle llama onBack: se limpia el panel
                detail = { id -> ReservationDetailRoute(reservationId = id, onBack = { selected = null }) },
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
