package org.vicpoo.gasolineproject.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.nav_account
import gasolineproject.shared.generated.resources.nav_fleet
import gasolineproject.shared.generated.resources.nav_home
import gasolineproject.shared.generated.resources.nav_requests
import gasolineproject.shared.generated.resources.nav_trips
import gasolineproject.shared.generated.resources.nav_history
import org.jetbrains.compose.resources.StringResource
import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.feature.profile.presentation.navigation.ProfileDestination
import org.vicpoo.gasolineproject.feature.reservations.presentation.navigation.CompanyReservationsDestination
import org.vicpoo.gasolineproject.feature.reservations.presentation.navigation.CompanyReservationsGraph
import org.vicpoo.gasolineproject.feature.profile.presentation.navigation.ProfileGraph
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
 * RBAC: una pestaña se muestra si el usuario tiene AL MENOS UNO de [requiredAnyOf]
 * (vacío = siempre visible). Para agregar una pestaña nueva solo se agrega un valor aquí
 * y su subgrafo en MainScreen.
 */
enum class TopLevelDestination(
    /** Ruta del subgrafo (navigation<Graph>). */
    val graph: Any,
    /** Pantalla inicial del subgrafo: en ella se ve la barra de pestañas. */
    val start: Any,
    /** Nombre de la pestaña (la barra solo muestra el ícono; el nombre es para accesibilidad). */
    val label: StringResource,
    val icon: () -> ImageVector,
    val requiredAnyOf: Set<String>,
) {
    /**
     * Casa: los carros de la empresa (carro.ver). En el detalle se manda la solicitud
     * (solicitud.crear) y arriba está la campana de "Mis solicitudes" (solicitud.ver_propias).
     */
    HOME(
        graph = CatalogGraph,
        start = CatalogDestination.List,
        label = Res.string.nav_home,
        icon = { AppIcons.Home },
        requiredAnyOf = setOf(Privileges.CARRO_VER),
    ),

    /** Carro: viajes (registrar, pausar, terminar y cargas). */
    TRIP_RECORDING(
        graph = TripRecordingGraph,
        start = TripRecordingDestination.AssignedVehicles,
        label = Res.string.nav_trips,
        icon = { AppIcons.Car },
        requiredAnyOf = setOf(Privileges.VIAJE_REGISTRAR),
    ),

    /** Libreta con lápiz: gestión de carros (ver, agregar, editar, eliminar). */
    FLEET(
        graph = VehiclesGraph,
        start = VehiclesDestination.List,
        label = Res.string.nav_fleet,
        icon = { AppIcons.Fleet },
        requiredAnyOf = setOf(Privileges.CARRO_CREAR, Privileges.CARRO_EDITAR, Privileges.CARRO_ELIMINAR),
    ),

    /** Llave: solicitudes de la empresa (aceptar / rechazar). */
    REQUESTS(
        graph = CompanyReservationsGraph,
        start = CompanyReservationsDestination.List,
        label = Res.string.nav_requests,
        icon = { AppIcons.Requests },
        requiredAnyOf = setOf(Privileges.SOLICITUD_VER_TODAS),
    ),

    /** Hoja con reloj: historial de viajes. */
    HISTORY(
        graph = TripHistoryGraph,
        start = TripHistoryDestination.List,
        label = Res.string.nav_history,
        icon = { AppIcons.History },
        requiredAnyOf = setOf(Privileges.VIAJE_VER_PROPIOS, Privileges.VIAJE_VER_TODOS),
    ),

    /** Persona: perfil (todos lo tienen). */
    PROFILE(
        graph = ProfileGraph,
        start = ProfileDestination.Overview,
        label = Res.string.nav_account,
        icon = { AppIcons.User },
        requiredAnyOf = emptySet(),
    ),
    ;

    fun isAllowed(privileges: Set<String>): Boolean =
        requiredAnyOf.isEmpty() || requiredAnyOf.any { it in privileges }
}
