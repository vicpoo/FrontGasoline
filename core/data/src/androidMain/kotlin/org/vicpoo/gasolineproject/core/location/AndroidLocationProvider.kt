package org.vicpoo.gasolineproject.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

/** Ubicación con el LocationManager del sistema (no requiere Google Play Services). */
class AndroidLocationProvider(private val context: Context) : LocationProvider {

    private val manager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun isLocationEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(manager)

    @SuppressLint("MissingPermission")
    override fun updates(intervalMillis: Long, minDistanceMeters: Float): Flow<LocationFix> {
        if (!hasPermission()) return emptyFlow()
        return callbackFlow {
            val listener = LocationListenerCompat { location -> trySend(location.toFix()) }
            val request = LocationRequestCompat.Builder(intervalMillis)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(minDistanceMeters)
                .build()
            val providers = activeProviders()
            try {
                providers.forEach { provider ->
                    LocationManagerCompat.requestLocationUpdates(
                        manager,
                        provider,
                        request,
                        ContextCompat.getMainExecutor(context),
                        listener,
                    )
                }
            } catch (e: SecurityException) {
                close() // el usuario quitó el permiso
            }
            awaitClose { LocationManagerCompat.removeUpdates(manager, listener) }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun lastKnown(): LocationFix? = withContext(Dispatchers.Default) {
        if (!hasPermission()) return@withContext null
        runCatching {
            activeProviders()
                .mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
                ?.toFix()
        }.getOrNull()
    }

    /** GPS para precisión; red como respaldo (llega antes en interiores). */
    private fun activeProviders(): List<String> {
        val all = manager.getProviders(true)
        val preferred = buildList {
            if (LocationManager.GPS_PROVIDER in all) add(LocationManager.GPS_PROVIDER)
            if (LocationManager.NETWORK_PROVIDER in all) add(LocationManager.NETWORK_PROVIDER)
            if (isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && LocationManager.FUSED_PROVIDER in all) {
                add(LocationManager.FUSED_PROVIDER)
            }
        }
        return preferred.ifEmpty { listOf(LocationManager.GPS_PROVIDER) }
    }
}

private fun Location.toFix(): LocationFix = LocationFix(
    lat = latitude,
    lng = longitude,
    accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else null,
    altitudeMeters = if (hasAltitude()) altitude else null,
    speedMetersPerSecond = if (hasSpeed()) speed.toDouble() else null,
    timeMillis = System.currentTimeMillis(),
)
