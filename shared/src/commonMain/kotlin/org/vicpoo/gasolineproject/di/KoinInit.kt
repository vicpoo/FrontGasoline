package org.vicpoo.gasolineproject.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.di.apiConfigModule
import org.vicpoo.gasolineproject.data.di.dataModules
import org.vicpoo.gasolineproject.domain.common.AppBuildInfo
import org.vicpoo.gasolineproject.domain.common.AppLog
import org.vicpoo.gasolineproject.domain.common.AppLogger
import org.vicpoo.gasolineproject.image.imageModule
import org.vicpoo.gasolineproject.domain.di.domainModules
import org.vicpoo.gasolineproject.navigation.di.navigationModule
import org.vicpoo.gasolineproject.platform.di.platformModule
import org.vicpoo.gasolineproject.ui.di.uiModules

/*
 * Composición de la app: el único lugar que junta las 4 capas.
 * Cada capa expone sus módulos de Koin (DSL); al agregar una feature, se agrega su módulo
 * en la lista de su capa (domainModules, dataModules, uiModules), no aquí.
 */
val appModules = listOf(platformModule) + domainModules + dataModules + uiModules + navigationModule + imageModule

/**
 * Se llama una vez al iniciar la app (Application en Android, MainViewController en iOS).
 * [apiBaseUrl]: URL de APIGasoline terminada en "/" (Android la toma de BuildConfig.API_BASE_URL).
 * [isDebug]: build de desarrollo (Android: BuildConfig.DEBUG). En release la bitácora solo escribe
 * avisos y errores, y Ktor no registra las peticiones.
 */
fun initKoin(apiBaseUrl: String, isDebug: Boolean, appDeclaration: KoinAppDeclaration = {}) {
    val koin = startKoin {
        appDeclaration()
        modules(appModules + apiConfigModule(apiBaseUrl) + module { single { AppBuildInfo(isDebug) } })
    }.koin
    // Para el código que no se crea con Koin (safeApiCall)
    AppLog.install(koin.get<AppLogger>())
}
