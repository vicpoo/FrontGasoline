package org.vicpoo.gasolineproject.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.vicpoo.gasolineproject.core.di.coreModule
import org.vicpoo.gasolineproject.core.di.networkModule
import org.vicpoo.gasolineproject.core.di.platformModule
import org.vicpoo.gasolineproject.core.di.rbacModule
import org.vicpoo.gasolineproject.core.di.sessionModule
import org.vicpoo.gasolineproject.core.di.syncModule
import org.vicpoo.gasolineproject.core.push.pushModule
import org.vicpoo.gasolineproject.core.rbac.rbacUiModule
import org.vicpoo.gasolineproject.feature.auth.di.authModule
import org.vicpoo.gasolineproject.feature.profile.di.profileModule
import org.vicpoo.gasolineproject.feature.reservations.di.reservationsModule
import org.vicpoo.gasolineproject.feature.triprecording.di.tripRecordingModule
import org.vicpoo.gasolineproject.feature.trips.di.tripsModule
import org.vicpoo.gasolineproject.feature.vehicles.di.vehiclesModule
import org.vicpoo.gasolineproject.navigation.di.navigationModule

/*
 * Composición de la app: el único lugar que conoce TODAS las features.
 * Vive fuera de core para que core no dependa de ninguna feature (en multimódulo, core no podría verlas).
 */

/** Infraestructura compartida (plataforma, sesión, red, RBAC, sincronización). */
private val infrastructureModules = listOf(
    platformModule,
    coreModule,
    sessionModule,
    networkModule,
    rbacModule,
    rbacUiModule,
    syncModule,
    pushModule,
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
