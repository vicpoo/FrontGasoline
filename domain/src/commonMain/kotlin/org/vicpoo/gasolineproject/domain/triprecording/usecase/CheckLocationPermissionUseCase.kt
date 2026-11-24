package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.location.LocationProvider

/** ¿Ya hay permiso de ubicación? (antes de iniciar el viaje se pide si no). */
class CheckLocationPermissionUseCase(private val locationProvider: LocationProvider) {
    operator fun invoke(): Boolean = locationProvider.hasPermission()
}
