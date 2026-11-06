package org.vicpoo.gasolineproject.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.local.JsonFileCache
import org.vicpoo.gasolineproject.data.trips.TripsRepositoryImpl
import org.vicpoo.gasolineproject.data.trips.TripsSnapshot
import org.vicpoo.gasolineproject.data.trips.remote.KtorTripsApi
import org.vicpoo.gasolineproject.data.trips.remote.TripsApi
import org.vicpoo.gasolineproject.domain.session.LogoutListener
import org.vicpoo.gasolineproject.domain.trips.TripsRepository

private val TRIPS_CACHE = named("tripsCache")

val tripsDataModule = module {
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
}
