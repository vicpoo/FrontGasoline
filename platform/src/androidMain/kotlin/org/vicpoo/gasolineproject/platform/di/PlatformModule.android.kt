package org.vicpoo.gasolineproject.platform.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vicpoo.gasolineproject.domain.session.LogoutListener
import org.vicpoo.gasolineproject.platform.files.AndroidTemporaryFiles
import org.vicpoo.gasolineproject.domain.common.AppBuildInfo
import org.vicpoo.gasolineproject.domain.common.AppLogger
import org.vicpoo.gasolineproject.domain.common.minLogLevel
import org.vicpoo.gasolineproject.domain.connectivity.ConnectivityMonitor
import org.vicpoo.gasolineproject.domain.files.AppDirectories
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController
import org.vicpoo.gasolineproject.domain.push.PushTokenProvider
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.platform.connectivity.AndroidConnectivityMonitor
import org.vicpoo.gasolineproject.platform.files.AndroidImageCompressor
import org.vicpoo.gasolineproject.platform.location.AndroidLocationProvider
import org.vicpoo.gasolineproject.platform.location.AndroidTrackingServiceController
import org.vicpoo.gasolineproject.platform.log.AndroidAppLogger
import org.vicpoo.gasolineproject.platform.push.AndroidPushTokenProvider
import org.vicpoo.gasolineproject.platform.sync.WorkManagerSyncScheduler

actual val platformModule: Module = module {
    // Bitácora (Logcat): en release solo avisos y errores. AppBuildInfo lo registra initKoin.
    single<AppLogger> { AndroidAppLogger(minLogLevel(get<AppBuildInfo>().isDebug)) }

    single {
        AppDirectories(
            filesDir = androidContext().filesDir.absolutePath,
            cacheDir = androidContext().cacheDir.absolutePath,
        )
    }

    // Temporales de cámara y reportes: lo viejo se borra al abrir la app (GasolineApplication) y todo al cerrar sesión
    single { AndroidTemporaryFiles(androidContext().cacheDir) }
    single<LogoutListener>(named("temporaryFiles")) {
        val files = get<AndroidTemporaryFiles>()
        LogoutListener { withContext(Dispatchers.IO) { files.clearAll() } }
    }
    single<ConnectivityMonitor> { AndroidConnectivityMonitor(androidContext()) }

    // GPS del viaje en curso
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single<ImageCompressor> { AndroidImageCompressor() }
    single<TrackingServiceController> { AndroidTrackingServiceController(androidContext()) }

    // Sincronización en segundo plano (WorkManager)
    single<BackgroundSyncScheduler> { WorkManagerSyncScheduler(androidContext()) }

    // Notificaciones push (Firebase Cloud Messaging)
    single<PushTokenProvider> { AndroidPushTokenProvider() }
}
