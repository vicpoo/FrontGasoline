package org.vicpoo.gasolineproject.platform.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.common.AppBuildInfo
import org.vicpoo.gasolineproject.domain.common.AppLogger
import org.vicpoo.gasolineproject.domain.common.minLogLevel
import org.vicpoo.gasolineproject.domain.connectivity.ConnectivityMonitor
import org.vicpoo.gasolineproject.domain.database.SqlDriverFactory
import org.vicpoo.gasolineproject.domain.device.ClientDeviceInfo
import org.vicpoo.gasolineproject.domain.files.AppDirectories
import org.vicpoo.gasolineproject.domain.files.FileCipher
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PlainFileCipher
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController
import org.vicpoo.gasolineproject.domain.push.PushTokenProvider
import org.vicpoo.gasolineproject.domain.session.CachedTokenStore
import org.vicpoo.gasolineproject.domain.session.TokenStore
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.domain.sync.usecase.SyncAllPendingUseCase
import org.vicpoo.gasolineproject.platform.connectivity.IosConnectivityMonitor
import org.vicpoo.gasolineproject.platform.database.IosSqlDriverFactory
import org.vicpoo.gasolineproject.platform.device.IosClientDeviceInfo
import org.vicpoo.gasolineproject.platform.files.IosImageCompressor
import org.vicpoo.gasolineproject.platform.location.IosLocationProvider
import org.vicpoo.gasolineproject.platform.location.IosTrackingServiceController
import org.vicpoo.gasolineproject.platform.log.IosAppLogger
import org.vicpoo.gasolineproject.platform.push.IosPushTokenProvider
import org.vicpoo.gasolineproject.platform.session.KeychainTokenStore
import org.vicpoo.gasolineproject.platform.sync.IosSyncScheduler
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSUserDomainMask

actual val platformModule: Module = module {
    // Bitácora (NSLog): en release solo avisos y errores. AppBuildInfo lo registra initKoin.
    single<AppLogger> { IosAppLogger(minLogLevel(get<AppBuildInfo>().isDebug)) }

    // TODO(iOS, punto 4.9): filesDir debería ir en Application Support y excluirse del respaldo de iCloud
    single { AppDirectories(filesDir = iosDirectoryPath(NSDocumentDirectory), cacheDir = iosDirectoryPath(NSCachesDirectory)) }
    single<FileSystem> { FileSystem.SYSTEM }
    single<ConnectivityMonitor> { IosConnectivityMonitor() }

    single<HttpClientEngine> { Darwin.create() }
    single<ClientDeviceInfo> { IosClientDeviceInfo() }

    // Tokens de la sesión en el Keychain (solo este dispositivo)
    single<TokenStore> { CachedTokenStore(KeychainTokenStore(json = get())) }

    // Base local (TODO iOS: sin cifrar por ahora)
    single<SqlDriverFactory> { IosSqlDriverFactory() }
    single<FileCipher> { PlainFileCipher } // TODO(iOS): cifrar con una llave del Keychain

    single<LocationProvider> { IosLocationProvider() }
    single<ImageCompressor> { IosImageCompressor() }
    single<TrackingServiceController> { IosTrackingServiceController() }
    single<BackgroundSyncScheduler> {
        IosSyncScheduler(appScope = get(), connectivity = get(), syncAll = { get<SyncAllPendingUseCase>() })
    }
    single<PushTokenProvider> { IosPushTokenProvider() }
}

@OptIn(ExperimentalForeignApi::class)
private fun iosDirectoryPath(directory: NSSearchPathDirectory): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = directory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
