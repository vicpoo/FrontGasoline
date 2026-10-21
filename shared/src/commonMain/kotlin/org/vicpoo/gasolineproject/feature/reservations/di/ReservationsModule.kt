package org.vicpoo.gasolineproject.feature.reservations.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.feature.reservations.data.ReservationRepositoryImpl
import org.vicpoo.gasolineproject.feature.reservations.data.local.CompanyReservationsSnapshot
import org.vicpoo.gasolineproject.feature.reservations.data.local.MyReservationsSnapshot
import org.vicpoo.gasolineproject.feature.reservations.data.remote.KtorReservationApi
import org.vicpoo.gasolineproject.feature.reservations.data.remote.ReservationApi
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

val reservationsModule = module {
    single<ReservationApi> { KtorReservationApi(get()) }
    single(MY_RESERVATIONS_CACHE) {
        JsonFileCache(get(), get(), "my_reservations.json", MyReservationsSnapshot.serializer(), get(), MyReservationsSnapshot())
    }
    single(COMPANY_RESERVATIONS_CACHE) {
        JsonFileCache(get(), get(), "company_reservations.json", CompanyReservationsSnapshot.serializer(), get(), CompanyReservationsSnapshot())
    }
    single<ReservationRepository> {
        ReservationRepositoryImpl(
            api = get(),
            sessionReader = get(),
            cache = get(MY_RESERVATIONS_CACHE),
            companyCache = get(COMPANY_RESERVATIONS_CACHE),
        )
    }

    // Al cerrar sesión se borran las solicitudes guardadas en el teléfono
    single<LogoutListener>(named("reservations")) {
        val repository = get<ReservationRepository>()
        LogoutListener { repository.clear() }
    }

    // Enviar solicitud (detalle del carro en Inicio)
    factoryOf(::RequestVehicleUseCase)

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
    viewModel { params ->
        ReservationDetailViewModel(
            reservationId = params.get(),
            observeRequest = get(),
            permissionChecker = get(),
            getDetail = get(),
            approveReservation = get(),
            rejectReservation = get(),
        )
    }
}
