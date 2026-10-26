package org.vicpoo.gasolineproject.core.push

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tipo de notificación (TipoNotificacion de APIGasoline, llega en el dato "tipo" del push). */
enum class PushType {
    /** A quien puede aceptar / rechazar: alguien de su empresa pidió un carro. */
    NEW_REQUEST,

    /** Al solicitante: su solicitud se aprobó o se rechazó. */
    REQUEST_RESULT,

    /** Cambio de estatus forzado de un carro. */
    VEHICLE_STATUS,
    OTHER,
    ;

    companion object {
        fun fromApi(value: String?): PushType = when (value) {
            "NuevaSolicitud" -> NEW_REQUEST
            "ResultadoSolicitud" -> REQUEST_RESULT
            "CambioEstatusForzado" -> VEHICLE_STATUS
            else -> OTHER
        }
    }
}

/** A qué se refiere un push: con esto se abre la pantalla correcta. */
data class PushTarget(
    val type: PushType,
    /** Id de la solicitud de reserva (referenciaId). */
    val referenceId: String?,
) {
    companion object {
        const val KEY_TYPE = "tipo"
        const val KEY_REFERENCE = "referenciaId"
        const val KEY_NOTIFICATION = "notificacionId"

        /** Lee los datos que manda la API (MensajesPush.Datos). null si no es un push de la app. */
        fun fromData(data: Map<String, String?>): PushTarget? {
            val type = data[KEY_TYPE] ?: return null
            return PushTarget(PushType.fromApi(type), data[KEY_REFERENCE]?.takeIf { it.isNotBlank() })
        }
    }
}

/** Token de Firebase del teléfono (implementación por plataforma). */
interface PushTokenProvider {
    /** "android" | "ios" (PlataformaDispositivo de la API). */
    val platform: String

    /** null si el teléfono no puede recibir push (sin Google Play Services, iOS pendiente...). */
    suspend fun currentToken(): String?

    /** Invalida el token: el celular deja de recibir los avisos del usuario anterior. */
    suspend fun deleteToken()
}

/**
 * Puente entre el sistema (servicio de Firebase, MainActivity) y la UI:
 * - [pendingOpen]: el usuario tocó una notificación; MainScreen navega y la consume.
 * - [received]: llegó un push con la app abierta; las listas se actualizan solas.
 */
class PushEvents {
    private val _pendingOpen = MutableStateFlow<PushTarget?>(null)
    val pendingOpen: StateFlow<PushTarget?> = _pendingOpen.asStateFlow()

    private val _received = MutableSharedFlow<PushTarget>(extraBufferCapacity = 16)
    val received: SharedFlow<PushTarget> = _received.asSharedFlow()

    fun open(target: PushTarget) {
        _pendingOpen.value = target
    }

    fun consumeOpen() {
        _pendingOpen.value = null
    }

    fun notifyReceived(target: PushTarget) {
        _received.tryEmit(target)
    }
}

/**
 * Pide el permiso de notificaciones una vez (Android 13+). En iOS no hace nada por ahora.
 * Se llama desde MainScreen para que también lo vea quien nunca inicia un viaje.
 */
@Composable
expect fun RequestNotificationPermission()
