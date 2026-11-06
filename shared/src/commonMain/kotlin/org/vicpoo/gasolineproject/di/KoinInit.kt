package org.vicpoo.gasolineproject.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.vicpoo.gasolineproject.data.di.dataModules
import org.vicpoo.gasolineproject.domain.di.domainModules
import org.vicpoo.gasolineproject.navigation.di.navigationModule
import org.vicpoo.gasolineproject.platform.di.platformModule
import org.vicpoo.gasolineproject.ui.di.uiModules

/*
 * Composición de la app: el único lugar que junta las 4 capas.
 * Cada capa expone sus módulos de Koin (DSL); al agregar una feature, se agrega su módulo
 * en la lista de su capa (domainModules, dataModules, uiModules), no aquí.
 */
val appModules = listOf(platformModule) + domainModules + dataModules + uiModules + navigationModule

/** Se llama una vez al iniciar la app (Application en Android, MainViewController en iOS). */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModules)
    }
}
