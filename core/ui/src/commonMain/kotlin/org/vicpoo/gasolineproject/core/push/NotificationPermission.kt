package org.vicpoo.gasolineproject.core.push

import androidx.compose.runtime.Composable

/**
 * Pide el permiso de notificaciones una vez (Android 13+). En iOS no hace nada por ahora.
 * Se llama desde MainScreen para que también lo vea quien nunca inicia un viaje.
 */
@Composable
expect fun RequestNotificationPermission()
