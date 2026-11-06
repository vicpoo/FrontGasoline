package org.vicpoo.gasolineproject.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.local.JsonFileCache
import org.vicpoo.gasolineproject.data.reservations.AssignmentsRepositoryImpl
import org.vicpoo.gasolineproject.data.reservations.ReservationRepositoryImpl
import org.vicpoo.gasolineproject.data.reservations.local.CompanyReservationsSnapshot
import org.vicpoo.gasolineproject.data.reservations.local.MyReservationsSnapshot
import org.vicpoo.gasolineproject.data.reservations.local.ReservationOutbox
import org.vicpoo.gasolineproject.data.reservations.local.ReservationOutboxSnapshot
import org.vicpoo.gasolineproject.data.reservations.remote.AssignmentsApi
import org.vicpoo.gasolineproject.data.reservations.remote.KtorAssignmentsApi
import org.vicpoo.gasolineproject.data.reservations.remote.KtorReservationApi
import org.vicpoo.gasolineproject.data.reservations.remote.ReservationApi
import org.vicpoo.gasolineproject.data.reservations.sync.ReservationOutboxSyncer
import org.vicpoo.gasolineproject.domain.reservations.AssignmentsRepository
import org.vicpoo.gasolineproject.domain.reservations.ReservationRepository
import org.vicpoo.gasolineproject.domain.session.LogoutListener
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask

private val MY_RESERVATIONS_CACHE = named("myReservationsCache")
private val COMPANY_RESERVATIONS_CACHE = named("companyReservationsCache")
private val OUTBOX_CACHE = named("reservationOutboxCache")

val reservationsDataModule = module {
    singleOf(::KtorReservationApi) { bind<ReservationApi>() }
    single(MY_RESERVATIONS_CACHE) {
        JsonFileCache(get(), get(), "my_reservations.json", MyReservationsSnapshot.serializer(), get(), MyReservationsSnapshot())
    }
    single(COMPANY_RESERVATIONS_CACHE) {
        JsonFileCache(get(), get(), "company_reservations.json", CompanyReservationsSnapshot.serializer(), get(), CompanyReservationsSnapshot())
    }
    // Solicitudes hechas sin red, en su propio archivo
    single(OUTBOX_CACHE) {
        JsonFileCache(get(), get(), "reservation_outbox.json", ReservationOutboxSnapshot.serializer(), get(), ReservationOutboxSnapshot())
    }
    single { ReservationOutbox(get(OUTBOX_CACHE)) }
    single<ReservationRepository> {
        ReservationRepositoryImpl(
            api = get(),
            sessionReader = get(),
            cache = get(MY_RESERVATIONS_CACHE),
            companyCache = get(COMPANY_RESERVATIONS_CACHE),
            outbox = get(),
            clock = get(),
            scheduler = get(),
        )
    }
    // Envía la cola en segundo plano (WorkManager / al volver la red); al enviar, recarga "Mis solicitudes"
    single {
        val repository = get<ReservationRepository>()
        ReservationOutboxSyncer(
            outbox = get(),
            api = get(),
            sessionReader = get(),
            scheduler = get(),
            onSent = { repository.refreshMine() },
        )
    } bind PendingSyncTask::class

    // Al cerrar sesión se borran las solicitudes guardadas en el teléfono
    single<LogoutListener>(named("reservations")) {
        val repository = get<ReservationRepository>()
        LogoutListener { repository.clear() }
    }

    // Asignados: quién tiene cada carro
    singleOf(::KtorAssignmentsApi) { bind<AssignmentsApi>() }
    single<AssignmentsRepository> { AssignmentsRepositoryImpl(api = get(), reservations = get()) }
}
