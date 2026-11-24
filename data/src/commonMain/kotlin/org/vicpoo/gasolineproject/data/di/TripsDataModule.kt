package org.vicpoo.gasolineproject.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.local.LocalPagedList
import org.vicpoo.gasolineproject.domain.common.AppDates
import org.vicpoo.gasolineproject.data.trips.TripsRepositoryImpl
import org.vicpoo.gasolineproject.data.trips.TripEntity
import org.vicpoo.gasolineproject.data.trips.remote.KtorTripsApi
import org.vicpoo.gasolineproject.data.trips.remote.TripsApi
import org.vicpoo.gasolineproject.domain.session.LogoutListener
import org.vicpoo.gasolineproject.domain.trips.TripsRepository

private val TRIPS_LIST = named("tripsList")

val tripsDataModule = module {
    singleOf(::KtorTripsApi) { bind<TripsApi>() }
    // Historial: un viaje por fila, recorrido por llave (punto 1.6)
    single(TRIPS_LIST) {
        LocalPagedList(
            database = get(),
            name = "trips",
            serializer = TripEntity.serializer(),
            json = get(),
            idOf = { it.id },
            sortKeyOf = { AppDates.sortKey(it.startedAt) },
        )
    }
    single {
        TripsRepositoryImpl(
            api = get(),
            trips = get(TRIPS_LIST),
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
