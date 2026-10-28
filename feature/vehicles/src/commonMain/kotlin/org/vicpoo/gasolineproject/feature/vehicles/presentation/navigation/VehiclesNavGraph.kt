package org.vicpoo.gasolineproject.feature.vehicles.presentation.navigation

import androidx.compose.runtime.Composable
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
import org.vicpoo.gasolineproject.feature.vehicles.presentation.detail.VehicleDetailRoute
import org.vicpoo.gasolineproject.feature.vehicles.presentation.form.VehicleFormRoute
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehicleListRoute
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehiclesMode
import org.vicpoo.gasolineproject.feature.vehicles.resources.Res
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_select_hint

// ======================= Pestaña Inicio (casa): carros para solicitar =======================

/**
 * Subgrafo de la pestaña "Inicio": ver los carros de la empresa, su detalle y mandar la solicitud
 * con su motivo. Arriba a la derecha va lo que la app ponga en [topEndAction] (hoy la campana de
 * "Mis solicitudes", que es de la feature de reservas).
 */
@Serializable
data object CatalogGraph

sealed interface CatalogDestination {
    @Serializable
    data object List : CatalogDestination

    @Serializable
    data class Detail(val vehicleId: String) : CatalogDestination
}

/**
 * @param topEndAction botón de la esquina superior derecha de la lista (lo decide la app).
 * @param extraDestinations pantallas de otras features que deben vivir dentro de esta pestaña
 *   (para que la barra marque "Inicio" y el historial de la pestaña se conserve).
 */
fun NavGraphBuilder.catalogGraph(
    navController: NavHostController,
    topEndAction: (@Composable () -> Unit)? = null,
    extraDestinations: NavGraphBuilder.() -> Unit = {},
) {
    navigation<CatalogGraph>(startDestination = CatalogDestination.List) {

        privilegedComposable<CatalogDestination.List>(AppAccess.VEHICLE_LIST) {
            // Tablet horizontal: lista y detalle lado a lado; teléfono: se navega al detalle
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            val twoPanes = showsListAndDetail
            ListDetailLayout(
                selectedId = selected,
                emptyDetailText = stringResource(Res.string.vehicle_select_hint),
                detailAccess = AppAccess.VEHICLE_DETAIL,
                onOpenSinglePane = { id ->
                    selected = null
                    navController.navigate(CatalogDestination.Detail(id))
                },
                onCloseDetail = { selected = null },
                list = {
                    VehicleListRoute(
                        mode = VehiclesMode.CATALOG,
                        onVehicleClick = { id ->
                            if (twoPanes) selected = id else navController.navigate(CatalogDestination.Detail(id))
                        },
                        onAddVehicle = {},
                        topEndAction = topEndAction,
                    )
                },
                detail = { id ->
                    VehicleDetailRoute(
                        vehicleId = id,
                        mode = VehiclesMode.CATALOG,
                        onBack = { selected = null },
                        onEdit = {},
                    )
                },
            )
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

        extraDestinations()
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
            // Tablet horizontal: lista y detalle lado a lado; teléfono: se navega al detalle
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            val twoPanes = showsListAndDetail
            ListDetailLayout(
                selectedId = selected,
                emptyDetailText = stringResource(Res.string.vehicle_select_hint),
                detailAccess = AppAccess.VEHICLE_DETAIL,
                onOpenSinglePane = { id ->
                    selected = null
                    navController.navigate(VehiclesDestination.Detail(id))
                },
                onCloseDetail = { selected = null },
                list = {
                    VehicleListRoute(
                        mode = VehiclesMode.FLEET,
                        onVehicleClick = { id ->
                            if (twoPanes) selected = id else navController.navigate(VehiclesDestination.Detail(id))
                        },
                        onAddVehicle = { navController.navigate(VehiclesDestination.Form()) },
                    )
                },
                detail = { id ->
                    VehicleDetailRoute(
                        vehicleId = id,
                        mode = VehiclesMode.FLEET,
                        // Al eliminarlo (o cerrar) se limpia el panel
                        onBack = { selected = null },
                        onEdit = { vehicleId -> navController.navigate(VehiclesDestination.Form(vehicleId)) },
                    )
                },
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
