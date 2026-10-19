package org.vicpoo.gasolineproject.core.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.vicpoo.gasolineproject.feature.auth.di.authModule
import org.vicpoo.gasolineproject.feature.profile.di.profileModule
import org.vicpoo.gasolineproject.feature.reservations.reservationsModule
import org.vicpoo.gasolineproject.feature.trips.di.tripsModule
import org.vicpoo.gasolineproject.feature.vehicles.di.vehiclesModule
import org.vicpoo.gasolineproject.navigation.navigationModule

/** Todos los módulos de la app. Al agregar una feature, agrega su módulo aquí. */
val appModules = listOf(
    platformModule,
    coreModule,
    navigationModule,
    authModule,
    profileModule,
    vehiclesModule,
    reservationsModule,
    tripsModule,
)

/** Se llama una vez al iniciar la app (Application en Android, MainViewController en iOS). */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModules)
    }
}
