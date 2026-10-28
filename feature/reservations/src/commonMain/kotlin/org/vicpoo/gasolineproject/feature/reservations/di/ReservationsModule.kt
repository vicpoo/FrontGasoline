package org.vicpoo.gasolineproject.feature.reservations.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.core.sync.PendingSyncTask
import org.vicpoo.gasolineproject.feature.reservations.api.VehicleRequester
import org.vicpoo.gasolineproject.feature.reservations.data.ReservationRepositoryImpl
import org.vicpoo.gasolineproject.feature.reservations.data.local.CompanyReservationsSnapshot
import org.vicpoo.gasolineproject.feature.reservations.data.local.MyReservationsSnapshot
import org.vicpoo.gasolineproject.feature.reservations.data.local.ReservationOutbox
import org.vicpoo.gasolineproject.feature.reservations.data.local.ReservationOutboxSnapshot
import org.vicpoo.gasolineproject.feature.reservations.data.remote.KtorReservationApi
import org.vicpoo.gasolineproject.feature.reservations.data.remote.ReservationApi
import org.vicpoo.gasolineproject.feature.reservations.data.sync.ReservationOutboxSyncer
import org.vicpoo.gasolineproject.feature.reservations.domain.ApproveReservationUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.GetReservationDetailUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.MarkReservationUpdatesSeenUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.ObserveCompanyReservationUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.ObserveCompanyReservationsUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.ObserveMyReservationsUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.ObserveUnseenReservationUpdatesUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.RefreshCompanyReservationsUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.RefreshMyReservationsUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.RefreshUnseenReservationUpdatesUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.RejectReservationUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.RequestVehicleUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRepository
import org.vicpoo.gasolineproject.feature.reservations.presentation.bell.ReservationsBellViewModel
import org.vicpoo.gasolineproject.feature.reservations.presentation.company.CompanyReservationsViewModel
import org.vicpoo.gasolineproject.feature.reservations.presentation.detail.ReservationDetailViewModel
import org.vicpoo.gasolineproject.feature.reservations.presentation.list.MyReservationsViewModel

private val MY_RESERVATIONS_CACHE = named("myReservationsCache")
private val COMPANY_RESERVATIONS_CACHE = named("companyReservationsCache")
private val OUTBOX_CACHE = named("reservationOutboxCache")

val reservationsModule = module {
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

    // Enviar solicitud (detalle del carro en Inicio)
    // Expuesto a otras features (vehículos) solo por su API pública
    factoryOf(::RequestVehicleUseCase) bind VehicleRequester::class

    // Campana: mis solicitudes y su estado
    factoryOf(::ObserveMyReservationsUseCase)
    factoryOf(::RefreshMyReservationsUseCase)
    factoryOf(::ObserveUnseenReservationUpdatesUseCase)
    factoryOf(::RefreshUnseenReservationUpdatesUseCase)
    factoryOf(::MarkReservationUpdatesSeenUseCase)
    viewModelOf(::MyReservationsViewModel)
    viewModelOf(::ReservationsBellViewModel)

    // Pestaña llave: solicitudes de la empresa y su detalle
    factoryOf(::ObserveCompanyReservationsUseCase)
    factoryOf(::RefreshCompanyReservationsUseCase)
    factoryOf(::ObserveCompanyReservationUseCase)
    factoryOf(::GetReservationDetailUseCase)
    factoryOf(::ApproveReservationUseCase)
    factoryOf(::RejectReservationUseCase)
    viewModelOf(::CompanyReservationsViewModel)
    viewModelOf(::ReservationDetailViewModel) // reservationId llega con parametersOf
}
