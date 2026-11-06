package org.vicpoo.gasolineproject.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.vehicles.local.JsonFileVehicleLocalDataSource
import org.vicpoo.gasolineproject.data.vehicles.local.VehicleLocalDataSource
import org.vicpoo.gasolineproject.data.vehicles.remote.KtorVehicleApi
import org.vicpoo.gasolineproject.data.vehicles.remote.VehicleApi
import org.vicpoo.gasolineproject.data.vehicles.repository.OfflineFirstVehicleRepository
import org.vicpoo.gasolineproject.data.vehicles.sync.VehicleSyncer
import org.vicpoo.gasolineproject.domain.session.LogoutListener
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.domain.vehicles.repository.VehicleReader
import org.vicpoo.gasolineproject.domain.vehicles.repository.VehicleRepository
import org.vicpoo.gasolineproject.domain.vehicles.repository.VehicleWriter

val vehiclesDataModule = module {
    // Autowire: Koin pasa cada parámetro del constructor por su tipo
    singleOf(::KtorVehicleApi) { bind<VehicleApi>() }
    singleOf(::JsonFileVehicleLocalDataSource) { bind<VehicleLocalDataSource>() }
    // También se registra como PendingSyncTask para la sincronización en segundo plano
    singleOf(::VehicleSyncer) { bind<PendingSyncTask>() }
    // Una sola instancia expuesta por sus tres interfaces (ISP: hay quien solo lee o solo escribe)
    singleOf(::OfflineFirstVehicleRepository) {
        bind<VehicleRepository>()
        bind<VehicleReader>()
        bind<VehicleWriter>()
    }

    // Al cerrar sesión se borran los vehículos guardados en el teléfono
    single<LogoutListener>(named("vehicles")) {
        val local = get<VehicleLocalDataSource>()
        LogoutListener { local.clearAll() }
    }
}
