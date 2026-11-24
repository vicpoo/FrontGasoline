package org.vicpoo.gasolineproject.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.local.LocalDocumentCache
import org.vicpoo.gasolineproject.data.local.LocalPagedList
import org.vicpoo.gasolineproject.data.reservations.local.ReservationRequestEntity
import org.koin.core.scope.Scope
import org.vicpoo.gasolineproject.data.reservations.AssignmentsRepositoryImpl
import org.vicpoo.gasolineproject.data.reservations.ReservationRepositoryImpl
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

private val MY_RESERVATIONS_LIST = named("myReservationsList")
private val COMPANY_RESERVATIONS_LIST = named("companyReservationsList")
private val UPDATES_CACHE = named("reservationUpdatesCache")
private val OUTBOX_CACHE = named("reservationOutboxCache")

val reservationsDataModule = module {
    singleOf(::KtorReservationApi) { bind<ReservationApi>() }
    // Mis solicitudes y las de la empresa: una por fila, recorridas por llave (punto 1.6)
    single(MY_RESERVATIONS_LIST) { reservationList(name = "my_reservations") }
    single(COMPANY_RESERVATIONS_LIST) { reservationList(name = "company_reservations") }
    // Solo el badge de la campana
    single(UPDATES_CACHE) {
        LocalDocumentCache(get(), get(), get(), "my_reservations.json", MyReservationsSnapshot.serializer(), get(), MyReservationsSnapshot())
    }
    // Solicitudes hechas sin red, en su propio archivo
    single(OUTBOX_CACHE) {
        LocalDocumentCache(get(), get(), get(), "reservation_outbox.json", ReservationOutboxSnapshot.serializer(), get(), ReservationOutboxSnapshot())
    }
    single { ReservationOutbox(get(OUTBOX_CACHE)) }
    single<ReservationRepository> {
        ReservationRepositoryImpl(
            api = get(),
            sessionReader = get(),
            mine = get(MY_RESERVATIONS_LIST),
            company = get(COMPANY_RESERVATIONS_LIST),
            updates = get(UPDATES_CACHE),
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

/** Solicitudes de reserva guardadas una por fila, de la más reciente a la más vieja. */
private fun Scope.reservationList(name: String) = LocalPagedList(
    database = get(),
    name = name,
    serializer = ReservationRequestEntity.serializer(),
    json = get(),
    idOf = { it.id },
    sortKeyOf = { it.requestedAt.orEmpty() },
)
