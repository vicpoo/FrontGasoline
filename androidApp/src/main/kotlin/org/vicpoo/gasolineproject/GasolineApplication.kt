package org.vicpoo.gasolineproject

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.vicpoo.gasolineproject.di.initKoin
import org.vicpoo.gasolineproject.core.push.PushNotifications

class GasolineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@GasolineApplication)
        }
        // Canal de las notificaciones push (debe existir antes de que llegue la primera)
        PushNotifications.createChannel(this)
    }
}
