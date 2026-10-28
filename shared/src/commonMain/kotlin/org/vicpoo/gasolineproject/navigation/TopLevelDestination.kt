package org.vicpoo.gasolineproject.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.nav_account
import gasolineproject.shared.generated.resources.nav_fleet
import gasolineproject.shared.generated.resources.nav_home
import gasolineproject.shared.generated.resources.nav_requests
import gasolineproject.shared.generated.resources.nav_trips
import gasolineproject.shared.generated.resources.nav_history
import gasolineproject.shared.generated.resources.nav_reports
import org.jetbrains.compose.resources.StringResource
import org.vicpoo.gasolineproject.core.rbac.AccessRule
import org.vicpoo.gasolineproject.core.rbac.AppAccess
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.feature.profile.presentation.navigation.ProfileDestination
import org.vicpoo.gasolineproject.feature.reservations.presentation.navigation.CompanyReservationsDestination
import org.vicpoo.gasolineproject.feature.reservations.presentation.navigation.CompanyReservationsGraph
import org.vicpoo.gasolineproject.feature.profile.presentation.navigation.ProfileGraph
import org.vicpoo.gasolineproject.feature.reports.presentation.navigation.ReportsDestination
import org.vicpoo.gasolineproject.feature.reports.presentation.navigation.ReportsGraph
import org.vicpoo.gasolineproject.feature.triprecording.presentation.navigation.TripRecordingDestination
import org.vicpoo.gasolineproject.feature.triprecording.presentation.navigation.TripRecordingGraph
import org.vicpoo.gasolineproject.feature.trips.presentation.navigation.TripHistoryDestination
import org.vicpoo.gasolineproject.feature.trips.presentation.navigation.TripHistoryGraph
import org.vicpoo.gasolineproject.feature.vehicles.presentation.navigation.CatalogDestination
import org.vicpoo.gasolineproject.feature.vehicles.presentation.navigation.CatalogGraph
import org.vicpoo.gasolineproject.feature.vehicles.presentation.navigation.VehiclesDestination
import org.vicpoo.gasolineproject.feature.vehicles.presentation.navigation.VehiclesGraph

/**
 * Pestañas de la pantalla principal. Cada una abre el subgrafo de su feature.
 * RBAC: una pestaña se muestra si el usuario cumple su [access]. Las reglas viven en
 * AppAccess (catálogo único). Para agregar una pestaña nueva se agrega un valor aquí,
 * su regla en AppAccess y su subgrafo en MainScreen.
 */
enum class TopLevelDestination(
    /** Ruta del subgrafo (navigation<Graph>). */
    val graph: Any,
    /** Pantalla inicial del subgrafo: en ella se ve la barra de pestañas. */
    val start: Any,
    /** Nombre de la pestaña (la barra solo muestra el ícono; el nombre es para accesibilidad). */
    val label: StringResource,
    val icon: () -> ImageVector,
    val access: AccessRule,
) {
    /**
     * Casa: los carros de la empresa para PEDIR uno. Se necesita carro.ver Y solicitud.crear:
     * quien solo administra carros (carro.ver + crear/editar/eliminar) ya los ve en la libreta
     * con lápiz, así no le salen dos pestañas de vehículos. En el detalle se manda la solicitud
     * y arriba está la campana de "Mis solicitudes" (solicitud.ver_propias).
     */
    HOME(
        graph = CatalogGraph,
        start = CatalogDestination.List,
        label = Res.string.nav_home,
        icon = { AppIcons.Home },
        access = AppAccess.HOME_TAB,
    ),

    /** Carro: viajes (registrar, pausar, terminar y cargas). */
    TRIP_RECORDING(
        graph = TripRecordingGraph,
        start = TripRecordingDestination.AssignedVehicles,
        label = Res.string.nav_trips,
        icon = { AppIcons.Car },
        access = AppAccess.TRIP_RECORDING,
    ),

    /** Libreta con lápiz: gestión de carros (ver, agregar, editar, eliminar). */
    FLEET(
        graph = VehiclesGraph,
        start = VehiclesDestination.List,
        label = Res.string.nav_fleet,
        icon = { AppIcons.Fleet },
        access = AppAccess.FLEET_TAB,
    ),

    /** Llave: solicitudes de la empresa (aceptar / rechazar). */
    REQUESTS(
        graph = CompanyReservationsGraph,
        start = CompanyReservationsDestination.List,
        label = Res.string.nav_requests,
        icon = { AppIcons.Requests },
        access = AppAccess.COMPANY_REQUESTS,
    ),

    /** Hoja con reloj: historial de viajes. */
    HISTORY(
        graph = TripHistoryGraph,
        start = TripHistoryDestination.List,
        label = Res.string.nav_history,
        icon = { AppIcons.History },
        access = AppAccess.TRIP_HISTORY,
    ),

    /** Gráfica: reportes de la flota (km, cargas y gasto; PDF / Excel). Módulo de auditoría. */
    REPORTS(
        graph = ReportsGraph,
        start = ReportsDestination.Fleet,
        label = Res.string.nav_reports,
        icon = { AppIcons.Chart },
        access = AppAccess.REPORTS,
    ),

    /** Persona: perfil (todos lo tienen). */
    PROFILE(
        graph = ProfileGraph,
        start = ProfileDestination.Overview,
        label = Res.string.nav_account,
        icon = { AppIcons.User },
        access = AppAccess.PROFILE,
    ),
    ;

    fun isAllowed(privileges: Set<String>): Boolean = access.isSatisfiedBy(privileges)
}
