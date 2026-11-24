package org.vicpoo.gasolineproject.domain.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.trips.usecase.LoadMoreTripsUseCase
import org.vicpoo.gasolineproject.domain.trips.usecase.LoadTripDetailsUseCase
import org.vicpoo.gasolineproject.domain.trips.usecase.ObserveTripUseCase
import org.vicpoo.gasolineproject.domain.trips.usecase.ObserveTripsUseCase
import org.vicpoo.gasolineproject.domain.trips.usecase.RefreshTripsUseCase

val tripsDomainModule = module {
    factoryOf(::ObserveTripsUseCase)
    factoryOf(::ObserveTripUseCase)
    factoryOf(::RefreshTripsUseCase)
    factoryOf(::LoadTripDetailsUseCase)
    factoryOf(::LoadMoreTripsUseCase)
}
