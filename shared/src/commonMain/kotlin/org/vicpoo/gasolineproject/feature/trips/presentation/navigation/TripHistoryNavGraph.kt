package org.vicpoo.gasolineproject.feature.trips.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.navigation.privilegedComposable
import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.feature.trips.presentation.detail.TripDetailRoute
import org.vicpoo.gasolineproject.feature.trips.presentation.history.TripHistoryRoute

/** Subgrafo de la pestaña "Historial". */
@Serializable
data object TripHistoryGraph

sealed interface TripHistoryDestination {
    @Serializable
    data object List : TripHistoryDestination

    @Serializable
    data class Detail(val tripId: String) : TripHistoryDestination
}

private val HISTORY_PRIVILEGES = setOf(Privileges.VIAJE_VER_PROPIOS, Privileges.VIAJE_VER_TODOS)

fun NavGraphBuilder.tripHistoryGraph(navController: NavHostController) {
    navigation<TripHistoryGraph>(startDestination = TripHistoryDestination.List) {

        privilegedComposable<TripHistoryDestination.List>(HISTORY_PRIVILEGES) {
            TripHistoryRoute(onTripClick = { id -> navController.navigate(TripHistoryDestination.Detail(id)) })
        }

        // Detalle del viaje (datos, mapa y cargas): viaje.ver_detalle
        privilegedComposable<TripHistoryDestination.Detail>(
            privileges = setOf(Privileges.VIAJE_VER_DETALLE),
            onDenied = { navController.popBackStack() },
        ) { entry ->
            val route = entry.toRoute<TripHistoryDestination.Detail>()
            TripDetailRoute(tripId = route.tripId, onBack = { navController.popBackStack() })
        }
    }
}
