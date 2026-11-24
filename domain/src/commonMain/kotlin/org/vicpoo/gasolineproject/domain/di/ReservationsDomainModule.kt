package org.vicpoo.gasolineproject.domain.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.reservations.VehicleRequester
import org.vicpoo.gasolineproject.domain.reservations.usecase.ApproveReservationUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.GetAssignmentsUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.GetReservationDetailUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.LoadMoreCompanyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.LoadMoreMyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.MarkReservationUpdatesSeenUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.ObserveAssignmentDetailUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.ObserveCompanyReservationUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.ObserveCompanyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.ObserveMyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.ObserveUnseenReservationUpdatesUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.RefreshCompanyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.RefreshMyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.RefreshUnseenReservationUpdatesUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.RejectReservationUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.RequestVehicleUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.UnlinkAssignmentUseCase

val reservationsDomainModule = module {
    // Vehículos pide un carro solo por esta interfaz (no conoce el caso de uso de reservas)
    factoryOf(::RequestVehicleUseCase) bind VehicleRequester::class

    // Campana: mis solicitudes y su estado
    factoryOf(::ObserveMyReservationsUseCase)
    factoryOf(::RefreshMyReservationsUseCase)
    factoryOf(::LoadMoreMyReservationsUseCase)
    factoryOf(::ObserveUnseenReservationUpdatesUseCase)
    factoryOf(::RefreshUnseenReservationUpdatesUseCase)
    factoryOf(::MarkReservationUpdatesSeenUseCase)

    // Pestaña llave: solicitudes de la empresa y su detalle
    factoryOf(::ObserveCompanyReservationsUseCase)
    factoryOf(::RefreshCompanyReservationsUseCase)
    factoryOf(::LoadMoreCompanyReservationsUseCase)
    factoryOf(::ObserveCompanyReservationUseCase)
    factoryOf(::GetReservationDetailUseCase)
    factoryOf(::ApproveReservationUseCase)
    factoryOf(::RejectReservationUseCase)

    // Asignados: quién tiene cada carro y desvincular
    factoryOf(::GetAssignmentsUseCase)
    factoryOf(::UnlinkAssignmentUseCase)
    factoryOf(::ObserveAssignmentDetailUseCase)
}
