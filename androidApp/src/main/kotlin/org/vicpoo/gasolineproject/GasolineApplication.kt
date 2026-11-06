package org.vicpoo.gasolineproject

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import org.vicpoo.gasolineproject.di.initKoin
import org.vicpoo.gasolineproject.platform.push.PushNotifications

class GasolineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            // Solo errores: con el nivel por defecto Koin escribe una línea por cada definición al iniciar
            androidLogger(Level.ERROR)
            androidContext(this@GasolineApplication)
        }
        // Canal de las notificaciones push (debe existir antes de que llegue la primera)
        PushNotifications.createChannel(this)
    }
}
