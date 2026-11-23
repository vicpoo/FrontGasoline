package org.vicpoo.gasolineproject

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.platform.files.AndroidTemporaryFiles
import org.vicpoo.gasolineproject.di.initKoin
import org.vicpoo.gasolineproject.platform.push.PushNotifications

class GasolineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(apiBaseUrl = BuildConfig.API_BASE_URL, isDebug = BuildConfig.DEBUG) {
            // Solo errores: con el nivel por defecto Koin escribe una línea por cada definición al iniciar
            androidLogger(Level.ERROR)
            androidContext(this@GasolineApplication)
        }
        // Canal de las notificaciones push (debe existir antes de que llegue la primera)
        PushNotifications.createChannel(this)
        // Fotos de cámara y reportes compartidos que se quedaron de otras veces (en segundo plano)
        val koin = GlobalContext.get()
        koin.get<AppScope>().scope.launch(Dispatchers.IO) { koin.get<AndroidTemporaryFiles>().clearStale() }
    }
}
