package org.vicpoo.gasolineproject.platform.di

import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.connectivity.ConnectivityMonitor
import org.vicpoo.gasolineproject.domain.files.AppDirectories
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController
import org.vicpoo.gasolineproject.domain.push.PushTokenProvider
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.domain.sync.SyncAllPendingUseCase
import org.vicpoo.gasolineproject.platform.connectivity.IosConnectivityMonitor
import org.vicpoo.gasolineproject.platform.files.IosImageCompressor
import org.vicpoo.gasolineproject.platform.location.IosLocationProvider
import org.vicpoo.gasolineproject.platform.location.IosTrackingServiceController
import org.vicpoo.gasolineproject.platform.push.IosPushTokenProvider
import org.vicpoo.gasolineproject.platform.sync.IosSyncScheduler
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val platformModule: Module = module {
    single { AppDirectories(filesDir = iosDocumentsPath()) }
    single<ConnectivityMonitor> { IosConnectivityMonitor() }

    single<LocationProvider> { IosLocationProvider() }
    single<ImageCompressor> { IosImageCompressor() }
    single<TrackingServiceController> { IosTrackingServiceController() }
    single<BackgroundSyncScheduler> {
        IosSyncScheduler(appScope = get(), connectivity = get(), syncAll = { get<SyncAllPendingUseCase>() })
    }
    single<PushTokenProvider> { IosPushTokenProvider() }
}

@OptIn(ExperimentalForeignApi::class)
private fun iosDocumentsPath(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
