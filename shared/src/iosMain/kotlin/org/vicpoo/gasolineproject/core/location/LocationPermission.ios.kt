package org.vicpoo.gasolineproject.core.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** iOS: pendiente (CLLocationManager). Por ahora responde "sin permiso". */
@Composable
actual fun rememberLocationPermission(onResult: (granted: Boolean) -> Unit): LocationPermissionLauncher =
    remember { LocationPermissionLauncher { onResult(false) } }
