package org.vicpoo.gasolineproject.core.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.vicpoo.gasolineproject.feature.auth.di.authModule
import org.vicpoo.gasolineproject.feature.profile.di.profileModule
import org.vicpoo.gasolineproject.feature.reservations.di.reservationsModule
import org.vicpoo.gasolineproject.feature.triprecording.di.tripRecordingModule
import org.vicpoo.gasolineproject.feature.trips.di.tripsModule
import org.vicpoo.gasolineproject.feature.vehicles.di.vehiclesModule
import org.vicpoo.gasolineproject.navigation.di.navigationModule

/** Infraestructura compartida (plataforma, sesión, red, RBAC, sincronización). */
private val infrastructureModules = listOf(
    platformModule,
    coreModule,
    sessionModule,
    networkModule,
    rbacModule,
    syncModule,
)

/** Una entrada por feature. Al agregar una feature nueva, agrega aquí su módulo. */
private val featureModules = listOf(
    authModule,
    profileModule,
    vehiclesModule,
    reservationsModule,
    tripsModule,
    tripRecordingModule,
)

val appModules = infrastructureModules + featureModules + navigationModule

/** Se llama una vez al iniciar la app (Application en Android, MainViewController en iOS). */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModules)
    }
}
