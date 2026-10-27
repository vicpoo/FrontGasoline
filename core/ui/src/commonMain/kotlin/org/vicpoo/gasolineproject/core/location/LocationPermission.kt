package org.vicpoo.gasolineproject.core.location

import androidx.compose.runtime.Composable

fun interface LocationPermissionLauncher {
    fun launch()
}

/**
 * Pide permiso de ubicación precisa (y de notificaciones en Android 13+ para el aviso del viaje).
 * onResult(true) si se concedió la ubicación.
 */
@Composable
expect fun rememberLocationPermission(onResult: (granted: Boolean) -> Unit): LocationPermissionLauncher
