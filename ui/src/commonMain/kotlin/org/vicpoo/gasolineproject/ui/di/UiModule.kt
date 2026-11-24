package org.vicpoo.gasolineproject.ui.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.ui.auth.LoginViewModel
import org.vicpoo.gasolineproject.ui.common.rbac.AccessGateViewModel
import org.vicpoo.gasolineproject.ui.common.theme.ThemeViewModel
import org.vicpoo.gasolineproject.ui.license.LicenseGateViewModel
import org.vicpoo.gasolineproject.ui.license.LicenseViewModel
import org.vicpoo.gasolineproject.ui.profile.ProfileViewModel
import org.vicpoo.gasolineproject.ui.profile.editname.EditNameViewModel
import org.vicpoo.gasolineproject.ui.reports.ReportsViewModel
import org.vicpoo.gasolineproject.ui.reservations.assignmentdetail.AssignmentDetailViewModel
import org.vicpoo.gasolineproject.ui.reservations.assignments.AssignmentsViewModel
import org.vicpoo.gasolineproject.ui.reservations.bell.ReservationsBellViewModel
import org.vicpoo.gasolineproject.ui.reservations.company.CompanyReservationsViewModel
import org.vicpoo.gasolineproject.ui.reservations.detail.ReservationDetailViewModel
import org.vicpoo.gasolineproject.ui.reservations.list.MyReservationsViewModel
import org.vicpoo.gasolineproject.ui.triprecording.active.ActiveTripViewModel
import org.vicpoo.gasolineproject.ui.triprecording.assigned.AssignedVehiclesViewModel
import org.vicpoo.gasolineproject.ui.triprecording.finish.FinishTripViewModel
import org.vicpoo.gasolineproject.ui.triprecording.fuel.FuelRecordFormViewModel
import org.vicpoo.gasolineproject.ui.triprecording.start.StartTripViewModel
import org.vicpoo.gasolineproject.ui.triprecording.syncstatus.SyncStatusViewModel
import org.vicpoo.gasolineproject.ui.trips.detail.TripDetailViewModel
import org.vicpoo.gasolineproject.ui.trips.history.TripHistoryViewModel
import org.vicpoo.gasolineproject.ui.vehicles.detail.VehicleDetailViewModel
import org.vicpoo.gasolineproject.ui.vehicles.form.VehicleFormViewModel
import org.vicpoo.gasolineproject.ui.vehicles.list.VehicleListViewModel

/*
 * Capa ui: pantallas (Compose) y sus ViewModels. Los ViewModels hacen el trabajo del negocio con casos de
 * uso de domain. Además pueden leer cuatro interfaces de domain que solo informan y no tienen reglas
 * (punto 3.2.6): ConnectivityMonitor (hay red), LastRefreshStore ("actualizado hace"), PermissionChecker
 * (privilegios) y AppClock (hora). Nunca reciben nada de data ni repositorios.
 * Los Args (VehicleListArgs, StartTripArgs, ...) llegan con parametersOf(...) y Koin los toma por tipo.
 */

/** RequirePrivilege: una instancia por regla (la llave del ViewModel es AccessRule.key). */
val rbacUiModule = module {
    viewModel { params -> AccessGateViewModel(permissionChecker = get(), rule = params.get()) }
}

/** Apariencia de toda la app (claro / oscuro / igual que el teléfono). */
val themeUiModule = module {
    viewModelOf(::ThemeViewModel)
}

val authUiModule = module {
    viewModelOf(::LoginViewModel)
}

val licenseUiModule = module {
    viewModelOf(::LicenseViewModel)
    viewModelOf(::LicenseGateViewModel)
}

val profileUiModule = module {
    viewModelOf(::ProfileViewModel)
    viewModelOf(::EditNameViewModel)
}

val vehiclesUiModule = module {
    viewModelOf(::VehicleListViewModel)
    viewModelOf(::VehicleDetailViewModel)
    viewModelOf(::VehicleFormViewModel)
}

val reservationsUiModule = module {
    viewModelOf(::MyReservationsViewModel)
    viewModelOf(::ReservationsBellViewModel)
    viewModelOf(::CompanyReservationsViewModel)
    viewModelOf(::ReservationDetailViewModel) // reservationId llega con parametersOf
    viewModelOf(::AssignmentsViewModel)
    viewModelOf(::AssignmentDetailViewModel) // la clave ("r:<reserva>" / "e:<empleado>") llega con parametersOf
}

val tripsUiModule = module {
    viewModelOf(::TripHistoryViewModel)
    viewModelOf(::TripDetailViewModel) // TripDetailArgs llega con parametersOf
}

val tripRecordingUiModule = module {
    viewModelOf(::AssignedVehiclesViewModel)
    viewModelOf(::ActiveTripViewModel)
    viewModelOf(::FinishTripViewModel)
    viewModelOf(::StartTripViewModel)
    viewModelOf(::FuelRecordFormViewModel)
    viewModelOf(::SyncStatusViewModel)
}

val reportsUiModule = module {
    viewModelOf(::ReportsViewModel)
}

/** Todos los módulos de la capa ui (los junta shared al iniciar Koin). */
val uiModules = listOf(
    rbacUiModule,
    themeUiModule,
    authUiModule,
    licenseUiModule,
    profileUiModule,
    vehiclesUiModule,
    reservationsUiModule,
    tripsUiModule,
    tripRecordingUiModule,
    reportsUiModule,
)
