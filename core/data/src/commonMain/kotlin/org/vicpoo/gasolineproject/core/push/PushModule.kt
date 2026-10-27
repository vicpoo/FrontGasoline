package org.vicpoo.gasolineproject.core.push

import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.core.sync.PendingSyncTask

/**
 * Notificaciones push. El PushTokenProvider lo pone cada plataforma (platformModule).
 * createdAtStart: el registrador empieza a escuchar la sesión al abrir la app.
 */
val pushModule = module {
    single { PushEvents() }
    single(createdAtStart = true) {
        DeviceRegistrar(client = get(), tokenProvider = get(), sessionReader = get(), dataStore = get(), appScope = get())
    }
    single { DevicePushSyncTask(get()) } bind PendingSyncTask::class

    // Al cerrar sesión: el celular deja de recibir los avisos de este usuario
    single<LogoutListener>(named("push")) {
        val registrar = get<DeviceRegistrar>()
        LogoutListener { registrar.unregister() }
    }
}
