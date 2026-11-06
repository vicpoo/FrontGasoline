package org.vicpoo.gasolineproject.platform.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.vicpoo.gasolineproject.domain.location.LocationFix
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController

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
