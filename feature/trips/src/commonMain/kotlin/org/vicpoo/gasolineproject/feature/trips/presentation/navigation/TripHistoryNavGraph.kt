package org.vicpoo.gasolineproject.feature.trips.presentation.navigation

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
import org.vicpoo.gasolineproject.core.navigation.privilegedComposable
import org.vicpoo.gasolineproject.core.rbac.AppAccess
import org.vicpoo.gasolineproject.core.ui.adaptive.ListDetailLayout
import org.vicpoo.gasolineproject.core.ui.adaptive.showsListAndDetail
import org.vicpoo.gasolineproject.feature.trips.presentation.detail.TripDetailRoute
import org.vicpoo.gasolineproject.feature.trips.presentation.history.TripHistoryRoute
import org.vicpoo.gasolineproject.feature.trips.resources.Res
import org.vicpoo.gasolineproject.feature.trips.resources.trip_select_hint

/** Subgrafo de la pestaña "Historial". */
@Serializable
data object TripHistoryGraph

sealed interface TripHistoryDestination {
    @Serializable
    data object List : TripHistoryDestination

    @Serializable
    data class Detail(val tripId: String) : TripHistoryDestination
}

fun NavGraphBuilder.tripHistoryGraph(navController: NavHostController) {
    navigation<TripHistoryGraph>(startDestination = TripHistoryDestination.List) {

        privilegedComposable<TripHistoryDestination.List>(AppAccess.TRIP_HISTORY) {
            // Tablet horizontal: historial y detalle del viaje lado a lado
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            val twoPanes = showsListAndDetail
            ListDetailLayout(
                selectedId = selected,
                emptyDetailText = stringResource(Res.string.trip_select_hint),
                detailAccess = AppAccess.TRIP_DETAIL,
                onOpenSinglePane = { id ->
                    selected = null
                    navController.navigate(TripHistoryDestination.Detail(id))
                },
                onCloseDetail = { selected = null },
                list = {
                    TripHistoryRoute(
                        onTripClick = { id ->
                            if (twoPanes) selected = id else navController.navigate(TripHistoryDestination.Detail(id))
                        },
                    )
                },
                detail = { id -> TripDetailRoute(tripId = id, onBack = { selected = null }) },
            )
        }

        // Detalle del viaje (datos, mapa y cargas): viaje.ver_detalle
        privilegedComposable<TripHistoryDestination.Detail>(
            access = AppAccess.TRIP_DETAIL,
            onDenied = { navController.popBackStack() },
        ) { entry ->
            val route = entry.toRoute<TripHistoryDestination.Detail>()
            TripDetailRoute(tripId = route.tripId, onBack = { navController.popBackStack() })
        }
    }
}
