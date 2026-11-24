package org.vicpoo.gasolineproject.domain.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.vehicles.usecase.DeleteVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObservePendingVehicleSyncUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObserveVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObserveVehiclesUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.RefreshVehiclesUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.SaveVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormValidator

val vehiclesDomainModule = module {
    factoryOf(::VehicleFormValidator)
    factoryOf(::ObserveVehiclesUseCase)
    factoryOf(::ObserveVehicleUseCase)
    factoryOf(::ObservePendingVehicleSyncUseCase)
    factoryOf(::RefreshVehiclesUseCase)
    factoryOf(::SaveVehicleUseCase)
    factoryOf(::DeleteVehicleUseCase)
}
