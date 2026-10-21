package org.vicpoo.gasolineproject.core.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** iOS: pendiente (CLLocationManager). Por ahora no da ubicaciones; el viaje se registra sin ruta. */
class IosLocationProvider : LocationProvider {
    override fun hasPermission(): Boolean = false
    override fun isLocationEnabled(): Boolean = false
    override fun updates(intervalMillis: Long, minDistanceMeters: Float): Flow<LocationFix> = emptyFlow()
    override suspend fun lastKnown(): LocationFix? = null
}

class IosTrackingServiceController : TrackingServiceController {
    override fun start() = Unit
    override fun stop() = Unit
}

@Composable
actual fun rememberLocationPermission(onResult: (granted: Boolean) -> Unit): LocationPermissionLauncher =
    remember { LocationPermissionLauncher { onResult(false) } }
