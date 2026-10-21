package org.vicpoo.gasolineproject.core.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.common.AppClock
import org.vicpoo.gasolineproject.core.common.SystemAppClock
import org.vicpoo.gasolineproject.core.network.TokenProvider
import org.vicpoo.gasolineproject.core.network.UnauthorizedHandler
import org.vicpoo.gasolineproject.core.network.createHttpClient
import org.vicpoo.gasolineproject.core.network.httpEngine
import org.vicpoo.gasolineproject.core.rbac.PermissionChecker
import org.vicpoo.gasolineproject.core.rbac.SessionPermissionChecker
import org.vicpoo.gasolineproject.core.session.DataStoreSessionStore
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.core.session.SessionStore
import org.vicpoo.gasolineproject.core.session.SessionWriter

/*
 * Módulos de infraestructura compartida, uno por responsabilidad (SRP):
 * - coreModule:    JSON, scope de la app y reloj.
 * - sessionModule: sesión guardada en el teléfono (lectura / escritura por separado, ISP).
 * - networkModule: cliente Ktor con token y manejo del 401.
 * - rbacModule:    consulta de privilegios.
 * Las features solo piden interfaces; nunca crean estas clases (DIP).
 */

/** Dependencias propias de cada plataforma (carpetas, archivos, DataStore, GPS, cámara...). */
expect val platformModule: Module

/** Scope de toda la app: para sincronizar en segundo plano aunque la pantalla se cierre. */
class AppScope(val scope: CoroutineScope)

val coreModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    single { AppScope(CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    single<AppClock> { SystemAppClock() }
}

val sessionModule = module {
    // Una sola instancia expuesta por sus interfaces
    single<SessionStore> { DataStoreSessionStore(dataStore = get(), json = get()) }
    single<SessionReader> { get<SessionStore>() }
    single<SessionWriter> { get<SessionStore>() }
}

val networkModule = module {
    single<TokenProvider> {
        val reader = get<SessionReader>()
        TokenProvider { reader.current()?.token }
    }
    single<UnauthorizedHandler> {
        val writer = get<SessionWriter>()
        UnauthorizedHandler { writer.clear() } // token vencido: se cierra la sesión
    }
    single {
        createHttpClient(
            engine = httpEngine(),
            tokenProvider = get(),
            unauthorizedHandler = get(),
            json = get(),
        )
    }
}

val rbacModule = module {
    single<PermissionChecker> { SessionPermissionChecker(get()) }
}
