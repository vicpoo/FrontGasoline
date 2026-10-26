package org.vicpoo.gasolineproject.core.di

import okio.FileSystem
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.location.AndroidLocationProvider
import org.vicpoo.gasolineproject.core.location.AndroidTrackingServiceController
import org.vicpoo.gasolineproject.core.location.LocationProvider
import org.vicpoo.gasolineproject.core.location.TrackingServiceController
import org.vicpoo.gasolineproject.core.platform.AppDirectories
import org.vicpoo.gasolineproject.core.push.AndroidPushTokenProvider
import org.vicpoo.gasolineproject.core.push.PushTokenProvider
import org.vicpoo.gasolineproject.core.platform.AndroidImageCompressor
import org.vicpoo.gasolineproject.core.platform.ImageCompressor
import org.vicpoo.gasolineproject.core.session.DATASTORE_FILE_NAME
import org.vicpoo.gasolineproject.core.session.createDataStore
import org.vicpoo.gasolineproject.core.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.core.sync.WorkManagerSyncScheduler

actual val platformModule: Module = module {
    single { AppDirectories(filesDir = androidContext().filesDir.absolutePath) }
    single<FileSystem> { FileSystem.SYSTEM }
    single {
        val directories = get<AppDirectories>()
        createDataStore { "${directories.filesDir}/$DATASTORE_FILE_NAME" }
    }

    // GPS del viaje en curso
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single<ImageCompressor> { AndroidImageCompressor() }
    single<TrackingServiceController> { AndroidTrackingServiceController(androidContext()) }

    // Sincronización en segundo plano (WorkManager)
    single<BackgroundSyncScheduler> { WorkManagerSyncScheduler(androidContext()) }

    // Notificaciones push (Firebase Cloud Messaging)
    single<PushTokenProvider> { AndroidPushTokenProvider() }
}
