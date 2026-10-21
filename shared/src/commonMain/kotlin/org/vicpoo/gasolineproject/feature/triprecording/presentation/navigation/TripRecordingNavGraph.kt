package org.vicpoo.gasolineproject.feature.triprecording.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.navigation.privilegedComposable
import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.feature.auth.presentation.license.LicenseRoute
import org.vicpoo.gasolineproject.feature.auth.presentation.license.RequireLicense
import org.vicpoo.gasolineproject.feature.triprecording.presentation.active.ActiveTripRoute
import org.vicpoo.gasolineproject.feature.triprecording.presentation.assigned.AssignedVehiclesRoute
import org.vicpoo.gasolineproject.feature.triprecording.presentation.finish.FinishTripRoute
import org.vicpoo.gasolineproject.feature.triprecording.presentation.fuel.FuelRecordFormRoute
import org.vicpoo.gasolineproject.feature.triprecording.presentation.start.StartTripRoute

/** Subgrafo de la pestaña "Registrar" (viajes y recargas). */
@Serializable
data object TripRecordingGraph

sealed interface TripRecordingDestination {
    /** Carros asignados (inicio del subgrafo). */
    @Serializable
    data object AssignedVehicles : TripRecordingDestination

    @Serializable
    data class StartTrip(val reservationId: String) : TripRecordingDestination

    @Serializable
    data object ActiveTrip : TripRecordingDestination

    /** "Registrar Viaje": kilometraje final, foto del odómetro y la ruta. */
    @Serializable
    data object FinishTrip : TripRecordingDestination

    /** recordId == null -> Agregar; != null -> Editar. */
    @Serializable
    data class FuelRecordForm(val recordId: String? = null) : TripRecordingDestination

    /** Si maneja y aún no sube su licencia, se le pide antes de ver sus carros. */
    @Serializable
    data object UploadLicense : TripRecordingDestination
}

private val TRIP_PRIVILEGES = setOf(Privileges.VIAJE_REGISTRAR)

fun NavGraphBuilder.tripRecordingGraph(navController: NavHostController) {
    navigation<TripRecordingGraph>(startDestination = TripRecordingDestination.AssignedVehicles) {

        privilegedComposable<TripRecordingDestination.AssignedVehicles>(TRIP_PRIVILEGES) {
            // Entran los que pueden iniciar un viaje Y ya subieron su licencia
            RequireLicense(onUploadLicense = { navController.navigate(TripRecordingDestination.UploadLicense) }) {
                AssignedVehiclesRoute(
                    onStartTrip = { id -> navController.navigate(TripRecordingDestination.StartTrip(id)) },
                    onOpenActiveTrip = {
                        navController.navigate(TripRecordingDestination.ActiveTrip) { launchSingleTop = true }
                    },
                )
            }
        }

        privilegedComposable<TripRecordingDestination.UploadLicense>(
            privileges = TRIP_PRIVILEGES,
            onDenied = { navController.popBackStack() },
        ) {
            LicenseRoute(
                isUpdate = true,
                onBack = { navController.popBackStack() },
                onGoToHome = { navController.popBackStack() },
            )
        }

        privilegedComposable<TripRecordingDestination.StartTrip>(
            privileges = TRIP_PRIVILEGES,
            onDenied = { navController.popBackStack() },
        ) { entry ->
            val route = entry.toRoute<TripRecordingDestination.StartTrip>()
            StartTripRoute(
                reservationId = route.reservationId,
                onBack = { navController.popBackStack() },
                // "Iniciar Viaje" se reemplaza por el mapa: atrás regresa a la lista de carros
                onTripStarted = {
                    navController.navigate(TripRecordingDestination.ActiveTrip) {
                        popUpTo<TripRecordingDestination.StartTrip> { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        privilegedComposable<TripRecordingDestination.ActiveTrip>(
            privileges = TRIP_PRIVILEGES,
            onDenied = { navController.popBackStack() },
        ) {
            ActiveTripRoute(
                onBack = { navController.popBackStack() },
                onAddFuel = { navController.navigate(TripRecordingDestination.FuelRecordForm()) },
                onEditFuel = { id -> navController.navigate(TripRecordingDestination.FuelRecordForm(id)) },
                onOpenFinish = {
                    navController.navigate(TripRecordingDestination.FinishTrip) { launchSingleTop = true }
                },
            )
        }

        privilegedComposable<TripRecordingDestination.FinishTrip>(
            privileges = TRIP_PRIVILEGES,
            onDenied = { navController.popBackStack() },
        ) {
            FinishTripRoute(
                // Atrás sin guardar: regresa al mapa y el viaje continúa
                onBack = { navController.popBackStack() },
                // Guardado: regresa a la lista de carros (sin el mapa del viaje en el historial)
                onDone = {
                    navController.popBackStack<TripRecordingDestination.AssignedVehicles>(inclusive = false)
                },
            )
        }

        privilegedComposable<TripRecordingDestination.FuelRecordForm>(
            privileges = setOf(Privileges.CARGA_REGISTRAR),
            onDenied = { navController.popBackStack() },
        ) { entry ->
            val route = entry.toRoute<TripRecordingDestination.FuelRecordForm>()
            FuelRecordFormRoute(recordId = route.recordId, onBack = { navController.popBackStack() })
        }
    }
}
