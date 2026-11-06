package org.vicpoo.gasolineproject.domain.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.auth.usecase.GetAuthStatusUseCase
import org.vicpoo.gasolineproject.domain.auth.usecase.LoginUseCase
import org.vicpoo.gasolineproject.domain.auth.usecase.LogoutUseCase
import org.vicpoo.gasolineproject.domain.auth.usecase.ObserveIsLoggedInUseCase
import org.vicpoo.gasolineproject.domain.auth.usecase.RefreshPrivilegesUseCase
import org.vicpoo.gasolineproject.domain.auth.validation.LoginValidator
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.common.SystemAppClock
import org.vicpoo.gasolineproject.domain.license.ObserveMustUploadLicenseUseCase
import org.vicpoo.gasolineproject.domain.license.UploadLicenseUseCase
import org.vicpoo.gasolineproject.domain.profile.ObserveProfileUseCase
import org.vicpoo.gasolineproject.domain.profile.RefreshProfileUseCase
import org.vicpoo.gasolineproject.domain.profile.UpdateNameUseCase
import org.vicpoo.gasolineproject.domain.profile.UpdateProfilePhotoUseCase
import org.vicpoo.gasolineproject.domain.push.PushEvents
import org.vicpoo.gasolineproject.domain.rbac.PermissionChecker
import org.vicpoo.gasolineproject.domain.rbac.SessionPermissionChecker
import org.vicpoo.gasolineproject.domain.reports.DownloadReportUseCase
import org.vicpoo.gasolineproject.domain.reports.ObserveReportUseCase
import org.vicpoo.gasolineproject.domain.reports.ObserveReportVehiclesUseCase
import org.vicpoo.gasolineproject.domain.reports.RefreshReportUseCase
import org.vicpoo.gasolineproject.domain.reservations.ApproveReservationUseCase
import org.vicpoo.gasolineproject.domain.reservations.GetAssignmentsUseCase
import org.vicpoo.gasolineproject.domain.reservations.GetReservationDetailUseCase
import org.vicpoo.gasolineproject.domain.reservations.MarkReservationUpdatesSeenUseCase
import org.vicpoo.gasolineproject.domain.reservations.ObserveAssignmentDetailUseCase
import org.vicpoo.gasolineproject.domain.reservations.ObserveCompanyReservationUseCase
import org.vicpoo.gasolineproject.domain.reservations.ObserveCompanyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.ObserveMyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.ObserveUnseenReservationUpdatesUseCase
import org.vicpoo.gasolineproject.domain.reservations.RefreshCompanyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.RefreshMyReservationsUseCase
import org.vicpoo.gasolineproject.domain.reservations.RefreshUnseenReservationUpdatesUseCase
import org.vicpoo.gasolineproject.domain.reservations.RejectReservationUseCase
import org.vicpoo.gasolineproject.domain.reservations.RequestVehicleUseCase
import org.vicpoo.gasolineproject.domain.reservations.UnlinkAssignmentUseCase
import org.vicpoo.gasolineproject.domain.reservations.VehicleRequester
import org.vicpoo.gasolineproject.domain.session.LogoutAction
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncCoordinator
import org.vicpoo.gasolineproject.domain.sync.CountPendingUseCase
import org.vicpoo.gasolineproject.domain.sync.ObservePendingSummaryUseCase
import org.vicpoo.gasolineproject.domain.sync.PendingBeforeLogoutUseCase
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.domain.sync.SyncAllPendingUseCase
import org.vicpoo.gasolineproject.domain.triprecording.CaptureFuelLocationUseCase
import org.vicpoo.gasolineproject.domain.triprecording.CheckLocationPermissionUseCase
import org.vicpoo.gasolineproject.domain.triprecording.DeleteFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripValidator
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordValidator
import org.vicpoo.gasolineproject.domain.triprecording.GetAssignedVehicleUseCase
import org.vicpoo.gasolineproject.domain.triprecording.GetFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.LocationPermissionChangedUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveCostCentersUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveFuelRecordsUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveReleaseBlocksUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveSyncStatusUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.domain.triprecording.PauseTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.RefreshAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.domain.triprecording.RefreshCostCentersUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ReleaseVehicleUseCase
import org.vicpoo.gasolineproject.domain.triprecording.ResumeTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.RetryFinishTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.RetryFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.SaveFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.StartTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.StopTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.SyncNowUseCase
import org.vicpoo.gasolineproject.domain.triprecording.SyncPendingTripsUseCase
import org.vicpoo.gasolineproject.domain.triprecording.UndoStopTripUseCase
import org.vicpoo.gasolineproject.domain.trips.LoadTripDetailsUseCase
import org.vicpoo.gasolineproject.domain.trips.ObserveTripUseCase
import org.vicpoo.gasolineproject.domain.trips.ObserveTripsUseCase
import org.vicpoo.gasolineproject.domain.trips.RefreshTripsUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.DeleteVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObservePendingVehicleSyncUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObserveVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObserveVehiclesUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.RefreshVehiclesUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.SaveVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormValidator

/*
 * Capa domain: reglas del negocio y casos de uso. Solo Kotlin puro (sin Ktor, DataStore, Android ni Compose).
 * Un módulo de Koin por feature; las implementaciones (repositorios, GPS, push...) las ponen data y platform.
 */

/** Piezas compartidas: scope de la app, reloj, privilegios, push y sincronización offline-first. */
val commonDomainModule = module {
    single { AppScope(CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    single<AppClock> { SystemAppClock() }
    single<PermissionChecker> { SessionPermissionChecker(get()) }
    single { PushEvents() }

    // Todas las features registradas con `bind PendingSyncTask::class`
    factory { SyncAllPendingUseCase(getAll<PendingSyncTask>()) }
    factory { PendingBeforeLogoutUseCase(getAll<PendingSyncTask>()) }
    factory { CountPendingUseCase(getAll<PendingSyncTask>()) }
    factory { ObservePendingSummaryUseCase(getAll<PendingSyncTask>()) }
    single(createdAtStart = true) {
        BackgroundSyncCoordinator(sessionReader = get(), scheduler = get(), connectivity = get(), appScope = get())
    }
}

val authDomainModule = module {
    factoryOf(::LoginValidator)
    factoryOf(::LoginUseCase)
    factoryOf(::RefreshPrivilegesUseCase)
    // Otras features (Perfil) cierran sesión por el contrato LogoutAction
    factoryOf(::LogoutUseCase) bind LogoutAction::class
    factoryOf(::GetAuthStatusUseCase)
    factoryOf(::ObserveIsLoggedInUseCase)
}

val licenseDomainModule = module {
    factoryOf(::UploadLicenseUseCase)
    factoryOf(::ObserveMustUploadLicenseUseCase)
}

val profileDomainModule = module {
    factoryOf(::ObserveProfileUseCase)
    factoryOf(::RefreshProfileUseCase)
    factoryOf(::UpdateNameUseCase)
    factoryOf(::UpdateProfilePhotoUseCase)
}

val vehiclesDomainModule = module {
    factoryOf(::VehicleFormValidator)
    factoryOf(::ObserveVehiclesUseCase)
    factoryOf(::ObserveVehicleUseCase)
    factoryOf(::ObservePendingVehicleSyncUseCase)
    factoryOf(::RefreshVehiclesUseCase)
    factoryOf(::SaveVehicleUseCase)
    factoryOf(::DeleteVehicleUseCase)
}

val reservationsDomainModule = module {
    // Vehículos pide un carro solo por esta interfaz (no conoce el caso de uso de reservas)
    factoryOf(::RequestVehicleUseCase) bind VehicleRequester::class

    // Campana: mis solicitudes y su estado
    factoryOf(::ObserveMyReservationsUseCase)
    factoryOf(::RefreshMyReservationsUseCase)
    factoryOf(::ObserveUnseenReservationUpdatesUseCase)
    factoryOf(::RefreshUnseenReservationUpdatesUseCase)
    factoryOf(::MarkReservationUpdatesSeenUseCase)

    // Pestaña llave: solicitudes de la empresa y su detalle
    factoryOf(::ObserveCompanyReservationsUseCase)
    factoryOf(::RefreshCompanyReservationsUseCase)
    factoryOf(::ObserveCompanyReservationUseCase)
    factoryOf(::GetReservationDetailUseCase)
    factoryOf(::ApproveReservationUseCase)
    factoryOf(::RejectReservationUseCase)

    // Asignados: quién tiene cada carro y desvincular
    factoryOf(::GetAssignmentsUseCase)
    factoryOf(::UnlinkAssignmentUseCase)
    factoryOf(::ObserveAssignmentDetailUseCase)
}

val tripsDomainModule = module {
    factoryOf(::ObserveTripsUseCase)
    factoryOf(::ObserveTripUseCase)
    factoryOf(::RefreshTripsUseCase)
    factoryOf(::LoadTripDetailsUseCase)
}

val tripRecordingDomainModule = module {
    factoryOf(::FuelRecordValidator)
    factoryOf(::ObserveAssignedVehiclesUseCase)
    factoryOf(::RefreshAssignedVehiclesUseCase)
    factoryOf(::GetAssignedVehicleUseCase)
    factoryOf(::ObserveReleaseBlocksUseCase)
    factoryOf(::ReleaseVehicleUseCase)
    factoryOf(::ObserveCostCentersUseCase)
    factoryOf(::RefreshCostCentersUseCase)
    factoryOf(::ObserveTrackingUseCase)
    factoryOf(::StartTripUseCase)
    factoryOf(::FinishTripValidator)
    factoryOf(::FinishTripUseCase)
    factoryOf(::RetryFinishTripUseCase)
    factoryOf(::PauseTripUseCase)
    factoryOf(::ResumeTripUseCase)
    factoryOf(::StopTripUseCase)
    factoryOf(::UndoStopTripUseCase)
    factoryOf(::ObserveFuelRecordsUseCase)
    factoryOf(::GetFuelRecordUseCase)
    factoryOf(::CaptureFuelLocationUseCase)
    factoryOf(::SaveFuelRecordUseCase)
    factoryOf(::RetryFuelRecordUseCase)
    factoryOf(::DeleteFuelRecordUseCase)
    factoryOf(::CheckLocationPermissionUseCase)
    factoryOf(::LocationPermissionChangedUseCase)
    factoryOf(::SyncPendingTripsUseCase)
    factoryOf(::ObserveSyncStatusUseCase)
    factoryOf(::SyncNowUseCase)
}

val reportsDomainModule = module {
    factoryOf(::ObserveReportUseCase)
    factoryOf(::ObserveReportVehiclesUseCase)
    factoryOf(::RefreshReportUseCase)
    factoryOf(::DownloadReportUseCase)
}

/** Todos los módulos de la capa domain (los junta shared al iniciar Koin). */
val domainModules = listOf(
    commonDomainModule,
    authDomainModule,
    licenseDomainModule,
    profileDomainModule,
    vehiclesDomainModule,
    reservationsDomainModule,
    tripsDomainModule,
    tripRecordingDomainModule,
    reportsDomainModule,
)
