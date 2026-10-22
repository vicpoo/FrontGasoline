package org.vicpoo.gasolineproject.feature.vehicles.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.navigation.privilegedComposable
import org.vicpoo.gasolineproject.core.rbac.AppAccess
import org.vicpoo.gasolineproject.feature.reservations.presentation.bell.ReservationsBell
import org.vicpoo.gasolineproject.feature.reservations.presentation.list.MyReservationsRoute
import org.vicpoo.gasolineproject.feature.vehicles.presentation.detail.VehicleDetailRoute
import org.vicpoo.gasolineproject.feature.vehicles.presentation.form.VehicleFormRoute
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehicleListRoute
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehiclesMode

// ======================= Pestaña Inicio (casa): carros para solicitar =======================

/**
 * Subgrafo de la pestaña "Inicio": ver los carros de la empresa, su detalle y mandar la solicitud
 * con su motivo. Arriba a la derecha está la campana con "Mis solicitudes" (solo su estado).
 */
@Serializable
data object CatalogGraph

sealed interface CatalogDestination {
    @Serializable
    data object List : CatalogDestination

    @Serializable
    data class Detail(val vehicleId: String) : CatalogDestination

    /** Campana: mis solicitudes y su estado. */
    @Serializable
    data object MyReservations : CatalogDestination
}

fun NavGraphBuilder.catalogGraph(navController: NavHostController) {
    navigation<CatalogGraph>(startDestination = CatalogDestination.List) {

        privilegedComposable<CatalogDestination.List>(AppAccess.VEHICLE_LIST) {
            VehicleListRoute(
                mode = VehiclesMode.CATALOG,
                onVehicleClick = { id -> navController.navigate(CatalogDestination.Detail(id)) },
                onAddVehicle = {},
                topEndAction = {
                    ReservationsBell(onClick = {
                        navController.navigate(CatalogDestination.MyReservations) { launchSingleTop = true }
                    })
                },
            )
        }

        privilegedComposable<CatalogDestination.MyReservations>(
            access = AppAccess.MY_RESERVATIONS,
            onDenied = { navController.popBackStack() },
        ) {
            MyReservationsRoute(onBack = { navController.popBackStack() })
        }

        privilegedComposable<CatalogDestination.Detail>(
            access = AppAccess.VEHICLE_DETAIL,
            onDenied = { navController.popBackStack() },
        ) { entry ->
            val route = entry.toRoute<CatalogDestination.Detail>()
            VehicleDetailRoute(
                vehicleId = route.vehicleId,
                mode = VehiclesMode.CATALOG,
                onBack = { navController.popBackStack() },
                onEdit = {},
            )
        }
    }
}

// ======================= Pestaña Gestión (libreta con lápiz): CRUD de carros =======================

/** Subgrafo de la pestaña "Gestión de carros": ver, agregar, editar y eliminar. */
@Serializable
data object VehiclesGraph

/** Pantallas del subgrafo (rutas tipadas). */
sealed interface VehiclesDestination {
    @Serializable
    data object List : VehiclesDestination

    @Serializable
    data class Detail(val vehicleId: String) : VehiclesDestination

    /** vehicleId == null -> Agregar; != null -> Editar */
    @Serializable
    data class Form(val vehicleId: String? = null) : VehiclesDestination
}

/**
 * Registra el subgrafo de gestión de vehículos. La feature es dueña de sus rutas y de cómo se
 * mueve entre sus pantallas; la app solo llama vehiclesGraph(...) (Open/Closed).
 */
fun NavGraphBuilder.vehiclesGraph(navController: NavHostController) {
    navigation<VehiclesGraph>(startDestination = VehiclesDestination.List) {

        privilegedComposable<VehiclesDestination.List>(AppAccess.VEHICLE_LIST) {
            VehicleListRoute(
                mode = VehiclesMode.FLEET,
                onVehicleClick = { id -> navController.navigate(VehiclesDestination.Detail(id)) },
                onAddVehicle = { navController.navigate(VehiclesDestination.Form()) },
            )
        }

        privilegedComposable<VehiclesDestination.Detail>(
            access = AppAccess.VEHICLE_DETAIL,
            onDenied = { navController.popBackStack() },
        ) { entry ->
            val route = entry.toRoute<VehiclesDestination.Detail>()
            VehicleDetailRoute(
                vehicleId = route.vehicleId,
                mode = VehiclesMode.FLEET,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(VehiclesDestination.Form(id)) },
            )
        }

        // Crear o editar: cualquiera de los dos privilegios abre el formulario;
        // el ViewModel y la API validan la acción concreta
        privilegedComposable<VehiclesDestination.Form>(
            access = AppAccess.VEHICLE_FORM,
            onDenied = { navController.popBackStack() },
        ) { entry ->
            val route = entry.toRoute<VehiclesDestination.Form>()
            VehicleFormRoute(
                vehicleId = route.vehicleId,
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
