package org.vicpoo.gasolineproject.domain.di

/*
 * Capa domain: reglas del negocio y casos de uso. Solo Kotlin puro (sin Ktor, DataStore, Android ni Compose).
 * Un archivo de Koin por feature (punto 3.2.5): CommonDomainModule.kt, AuthDomainModule.kt, ...
 * Las implementaciones (repositorios, GPS, push...) las ponen data y platform.
 */

/** Todos los módulos de la capa domain (los junta shared al iniciar Koin). */
val domainModules = listOf(
    commonDomainModule,
    authDomainModule,
    licenseDomainModule,
    profileDomainModule,
    settingsDomainModule,
    vehiclesDomainModule,
    reservationsDomainModule,
    tripsDomainModule,
    tripRecordingDomainModule,
    reportsDomainModule,
)
