package org.vicpoo.gasolineproject.feature.vehicles.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.session.LogoutListener
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
import org.vicpoo.gasolineproject.feature.vehicles.presentation.detail.VehicleDetailArgs
import org.vicpoo.gasolineproject.feature.vehicles.presentation.detail.VehicleDetailViewModel
import org.vicpoo.gasolineproject.feature.vehicles.presentation.form.VehicleFormArgs
import org.vicpoo.gasolineproject.feature.vehicles.presentation.form.VehicleFormViewModel
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehicleListArgs
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehicleListViewModel

val vehiclesModule = module {
    // Data
    single<VehicleApi> { KtorVehicleApi(get()) }
    single<VehicleLocalDataSource> { JsonFileVehicleLocalDataSource(fileSystem = get(), directories = get(), json = get()) }
    single { VehicleSyncer(local = get(), api = get()) }
    single<VehicleRepository> {
        OfflineFirstVehicleRepository(local = get(), api = get(), syncer = get(), sessionReader = get(), appScope = get())
    }
    single<VehicleReader> { get<VehicleRepository>() }
    single<VehicleWriter> { get<VehicleRepository>() }

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

    // Presentation
    viewModel { params ->
        VehicleListViewModel(
            args = params.get<VehicleListArgs>(),
            observeVehicles = get(),
            observePendingSync = get(),
            refreshVehicles = get(),
            permissionChecker = get(),
        )
    }
    viewModel { params ->
        VehicleDetailViewModel(
            args = params.get<VehicleDetailArgs>(),
            observeVehicle = get(),
            deleteVehicle = get(),
            requestVehicle = get(),
            permissionChecker = get(),
        )
    }
    viewModel { params ->
        VehicleFormViewModel(
            args = params.get<VehicleFormArgs>(),
            observeVehicle = get(),
            saveVehicle = get(),
            validator = get(),
        )
    }
}
