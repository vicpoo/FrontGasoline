package org.vicpoo.gasolineproject.feature.vehicles.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.core.sync.PendingSyncTask
import org.vicpoo.gasolineproject.feature.vehicles.data.local.JsonFileVehicleLocalDataSource
import org.vicpoo.gasolineproject.feature.vehicles.data.local.VehicleLocalDataSource
import org.vicpoo.gasolineproject.feature.vehicles.data.remote.KtorVehicleApi
import org.vicpoo.gasolineproject.feature.vehicles.data.remote.VehicleApi
import org.vicpoo.gasolineproject.feature.vehicles.data.repository.OfflineFirstVehicleRepository
import org.vicpoo.gasolineproject.feature.vehicles.data.sync.VehicleSyncer
import org.vicpoo.gasolineproject.feature.vehicles.domain.repository.VehicleReader
import org.vicpoo.gasolineproject.feature.vehicles.domain.repository.VehicleRepository
import org.vicpoo.gasolineproject.feature.vehicles.domain.repository.VehicleWriter
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.DeleteVehicleUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObservePendingVehicleSyncUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObserveVehicleUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObserveVehiclesUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.RefreshVehiclesUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.SaveVehicleUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.validation.VehicleFormValidator
import org.vicpoo.gasolineproject.feature.vehicles.presentation.detail.VehicleDetailViewModel
import org.vicpoo.gasolineproject.feature.vehicles.presentation.form.VehicleFormViewModel
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehicleListViewModel

val vehiclesModule = module {
    // Data (autowire: Koin pasa cada parámetro del constructor por su tipo)
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

    // Domain
    factoryOf(::VehicleFormValidator)
    factoryOf(::ObserveVehiclesUseCase)
    factoryOf(::ObserveVehicleUseCase)
    factoryOf(::ObservePendingVehicleSyncUseCase)
    factoryOf(::RefreshVehiclesUseCase)
    factoryOf(::SaveVehicleUseCase)
    factoryOf(::DeleteVehicleUseCase)

    // Presentation: los Args llegan con parametersOf(...) desde la pantalla y Koin los toma por tipo
    viewModelOf(::VehicleListViewModel)
    viewModelOf(::VehicleDetailViewModel)
    viewModelOf(::VehicleFormViewModel)
}
