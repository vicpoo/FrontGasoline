package org.vicpoo.gasolineproject.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.vicpoo.gasolineproject.core.di.AppScope

/**
 * Servicio de Firebase (declarado en el Manifest de androidApp):
 * - onNewToken: Firebase renovó el token → se vuelve a registrar el celular en la API.
 * - onMessageReceived: SOLO se llama con la app abierta (cerrada, Android muestra el push solo).
 *   Se muestra la notificación y se avisa a las pantallas para que se actualicen.
 */
class GasolineMessagingService : FirebaseMessagingService(), KoinComponent {

    private val registrar: DeviceRegistrar by inject()
    private val events: PushEvents by inject()
    private val appScope: AppScope by inject()

    override fun onNewToken(token: String) {
        appScope.scope.launch { registrar.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        PushTarget.fromData(data)?.let(events::notifyReceived)

        val title = message.notification?.title ?: data[KEY_TITLE] ?: return
        val body = message.notification?.body ?: data[KEY_BODY].orEmpty()
        PushNotifications.show(applicationContext, title, body, data)
    }

    private companion object {
        const val KEY_TITLE = "titulo"
        const val KEY_BODY = "cuerpo"
    }
}
