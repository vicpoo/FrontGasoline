package org.vicpoo.gasolineproject.navigation

import kotlinx.serialization.Serializable

/*
 * Rutas del NavHost RAÍZ (rutas tipadas con @Serializable).
 *
 *  Raíz
 *  ├── AuthGraph ............ (Welcome, desactivada) → Login → UploadLicense
 *  └── HomeRoute ............ contenedor con pestañas y su propio NavHost anidado:
 *       ├── CatalogGraph .............. List → Detail (solicitar)          🏠 Inicio      (feature/vehicles)
 *       ├── TripRecordingGraph ........ AssignedVehicles → StartTrip → ActiveTrip → FinishTrip / FuelRecordForm   🚗 Viajes
 *       ├── VehiclesGraph ............. List → Detail → Form (gestión)    📝 Gestión     (feature/vehicles)
 *       ├── CompanyReservationsGraph .. List (aceptar / rechazar)         🔑 Solicitudes (feature/reservations)
 *       ├── TripHistoryGraph .......... List → Detail                     🕒 Historial   (feature/trips)
 *       └── ProfileGraph .............. Overview → EditName / UpdateLicense / MyReservations   👤 Perfil
 *
 * Cada feature define sus propias rutas y su subgrafo (presentation/navigation).
 */

/** Subgrafo de autenticación (sin sesión o falta la licencia). */
@Serializable
data object AuthGraph

/**
 * La pantalla de bienvenida (Welcome) está DESACTIVADA: sin sesión el flujo es Splash -> Login.
 * La pantalla y su ruta se conservan; para volver a usarla basta con poner esto en true.
 */
const val SHOW_WELCOME_SCREEN = false

/** Primera pantalla sin sesión (y a donde se regresa al cerrar sesión). */
val AuthEntryDestination: AuthDestination
    get() = if (SHOW_WELCOME_SCREEN) AuthDestination.Welcome else AuthDestination.Login

sealed interface AuthDestination {
    @Serializable
    data object Welcome : AuthDestination

    @Serializable
    data object Login : AuthDestination

    @Serializable
    data object UploadLicense : AuthDestination
}

/** Pantalla principal con la navegación por pestañas (NavHost anidado). */
@Serializable
data object HomeRoute
