package org.vicpoo.gasolineproject.navigation

import kotlinx.serialization.Serializable

/** Destinos de navegación tipados (type-safe navigation). */
sealed interface AppRoute {
    @Serializable
    data object Welcome : AppRoute

    @Serializable
    data object Login : AppRoute

    @Serializable
    data object UploadLicense : AppRoute

    /** Pantalla principal con pestañas (Vehículos, Mi cuenta, ...). */
    @Serializable
    data object Home : AppRoute

    /** Editar nombre (desde el perfil). */
    @Serializable
    data object EditName : AppRoute

    /** Actualizar licencia de conducir (desde el perfil). */
    @Serializable
    data object UpdateLicense : AppRoute

    @Serializable
    data class TripDetail(val tripId: String) : AppRoute

    @Serializable
    data class VehicleDetail(val vehicleId: String) : AppRoute

    /** vehicleId == null -> Agregar; != null -> Editar */
    @Serializable
    data class VehicleForm(val vehicleId: String? = null) : AppRoute
}
