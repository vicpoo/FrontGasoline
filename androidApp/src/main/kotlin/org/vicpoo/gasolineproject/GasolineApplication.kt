package org.vicpoo.gasolineproject

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.vicpoo.gasolineproject.core.di.initKoin

class GasolineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@GasolineApplication)
        }
    }
}
