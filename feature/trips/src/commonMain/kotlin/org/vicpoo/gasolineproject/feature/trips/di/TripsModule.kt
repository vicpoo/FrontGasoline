package org.vicpoo.gasolineproject.feature.trips.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.feature.trips.data.TripsRepositoryImpl
import org.vicpoo.gasolineproject.feature.trips.data.TripsSnapshot
import org.vicpoo.gasolineproject.feature.trips.data.remote.KtorTripsApi
import org.vicpoo.gasolineproject.feature.trips.data.remote.TripsApi
import org.vicpoo.gasolineproject.feature.trips.domain.LoadTripDetailsUseCase
import org.vicpoo.gasolineproject.feature.trips.domain.ObserveTripUseCase
import org.vicpoo.gasolineproject.feature.trips.domain.ObserveTripsUseCase
import org.vicpoo.gasolineproject.feature.trips.domain.RefreshTripsUseCase
import org.vicpoo.gasolineproject.feature.trips.domain.TripsRepository
import org.vicpoo.gasolineproject.feature.trips.presentation.detail.TripDetailViewModel
import org.vicpoo.gasolineproject.feature.trips.presentation.history.TripHistoryViewModel

private val TRIPS_CACHE = named("tripsCache")

val tripsModule = module {
    // Data
    singleOf(::KtorTripsApi) { bind<TripsApi>() }
    single(TRIPS_CACHE) {
        JsonFileCache(
            fileSystem = get(),
            directories = get(),
            fileName = "trips.json",
            serializer = TripsSnapshot.serializer(),
            json = get(),
            emptyValue = TripsSnapshot(),
        )
    }
    single {
        TripsRepositoryImpl(
            api = get(),
            cache = get(TRIPS_CACHE),
            sessionReader = get(),
            permissionChecker = get(),
        )
    }
    single<TripsRepository> { get<TripsRepositoryImpl>() }

    // Al cerrar sesión se borra el historial guardado
    single<LogoutListener>(named("trips")) {
        val repository = get<TripsRepositoryImpl>()
        LogoutListener { repository.clear() }
    }

    // Domain
    factoryOf(::ObserveTripsUseCase)
    factoryOf(::ObserveTripUseCase)
    factoryOf(::RefreshTripsUseCase)
    factoryOf(::LoadTripDetailsUseCase)

    // Presentation
    viewModelOf(::TripHistoryViewModel)
    viewModelOf(::TripDetailViewModel) // TripDetailArgs llega con parametersOf
}
