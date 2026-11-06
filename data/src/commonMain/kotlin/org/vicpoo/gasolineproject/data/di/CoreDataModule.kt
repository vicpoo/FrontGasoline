package org.vicpoo.gasolineproject.data.di

import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.local.DataStoreLastRefreshStore
import org.vicpoo.gasolineproject.data.network.AuthTokenManager
import org.vicpoo.gasolineproject.data.network.KtorSessionApi
import org.vicpoo.gasolineproject.data.network.SessionApi
import org.vicpoo.gasolineproject.data.network.createHttpClient
import org.vicpoo.gasolineproject.data.network.createSessionHttpClient
import org.vicpoo.gasolineproject.data.network.httpEngine
import org.vicpoo.gasolineproject.data.push.DevicePushSyncTask
import org.vicpoo.gasolineproject.data.push.DeviceRegistrar
import org.vicpoo.gasolineproject.data.session.DATASTORE_FILE_NAME
import org.vicpoo.gasolineproject.data.session.DataStoreLocalDataOwnerStore
import org.vicpoo.gasolineproject.data.session.DataStoreSessionStore
import org.vicpoo.gasolineproject.data.session.createDataStore
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.LastRefreshStore
import org.vicpoo.gasolineproject.domain.files.AppDirectories
import org.vicpoo.gasolineproject.domain.push.PushDeviceRegistrar
import org.vicpoo.gasolineproject.domain.session.LocalDataOwnerStore
import org.vicpoo.gasolineproject.domain.session.LogoutListener
import org.vicpoo.gasolineproject.domain.session.SessionReader
import org.vicpoo.gasolineproject.domain.session.SessionStore
import org.vicpoo.gasolineproject.domain.session.SessionWriter
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask

/*
 * Capa data: implementa las interfaces de domain con red (Ktor), DataStore y archivos JSON.
 * - coreDataModule:    JSON, sesión guardada, "Actualizado hace X".
 * - networkDataModule: cliente Ktor con token y renovación automática de la sesión (refresh token).
 * - pushDataModule:    registro del celular en la API para recibir push.
 * - dataPlatformModule (por plataforma): archivos, DataStore y tokens cifrados (Keystore / Keychain).
 * Más un módulo por feature (AuthDataModule.kt, VehiclesDataModule.kt, ...).
 */

/** Archivos, DataStore y tokens cifrados: cambian entre Android e iOS. */
expect val dataPlatformModule: Module

val coreDataModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    single {
        val directories = get<AppDirectories>()
        createDataStore { "${directories.filesDir}/$DATASTORE_FILE_NAME" }
    }

    // Una sola instancia expuesta por sus interfaces
    single { DataStoreSessionStore(dataStore = get(), json = get()) } binds arrayOf(
        SessionStore::class,
        SessionReader::class,
        SessionWriter::class,
    )

    // De quién son los datos del teléfono (si entra otra cuenta no se mezclan)
    single<LocalDataOwnerStore> { DataStoreLocalDataOwnerStore(dataStore = get()) }

    // "Actualizado hace X" de cada lista; se borra al cerrar sesión
    single<LastRefreshStore> { DataStoreLastRefreshStore(dataStore = get(), clock = get()) }
    single<LogoutListener>(named("lastRefresh")) {
        val store = get<LastRefreshStore>()
        LogoutListener { store.clear() }
    }
}

val networkDataModule = module {
    // Renovar / cerrar sesión con el refresh token: cliente propio, sin AuthPlugin
    single<SessionApi> { KtorSessionApi(createSessionHttpClient(engine = httpEngine(), json = get())) }

    // Dueño de los tokens (TokenStore cifrado): da el JWT, lo renueva y cierra la sesión si la API lo rechaza
    single {
        val clock = get<AppClock>()
        AuthTokenManager(
            store = get(),
            api = get(),
            sessionWriter = get(),
            nowMillis = { clock.nowMillis() },
        )
    }
    single {
        val tokens = get<AuthTokenManager>()
        createHttpClient(
            engine = httpEngine(),
            tokenProvider = tokens,
            tokenRefresher = tokens,
            json = get(),
        )
    }
}

/** Notificaciones push. El PushTokenProvider lo pone platform. createdAtStart: escucha la sesión al abrir la app. */
val pushDataModule = module {
    single(createdAtStart = true) {
        DeviceRegistrar(client = get(), tokenProvider = get(), sessionReader = get(), dataStore = get(), appScope = get())
    } bind PushDeviceRegistrar::class
    single { DevicePushSyncTask(get()) } bind PendingSyncTask::class

    // Al cerrar sesión: el celular deja de recibir los avisos de este usuario
    single<LogoutListener>(named("push")) {
        val registrar = get<DeviceRegistrar>()
        LogoutListener { registrar.unregister() }
    }
}

/** Todos los módulos de la capa data (los junta shared al iniciar Koin). */
val dataModules = listOf(
    dataPlatformModule,
    coreDataModule,
    networkDataModule,
    pushDataModule,
    authDataModule,
    profileDataModule,
    vehiclesDataModule,
    reservationsDataModule,
    tripsDataModule,
    tripRecordingDataModule,
    reportsDataModule,
)
