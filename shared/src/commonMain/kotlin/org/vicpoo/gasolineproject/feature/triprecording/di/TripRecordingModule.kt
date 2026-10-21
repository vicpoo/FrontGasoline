package org.vicpoo.gasolineproject.feature.triprecording.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.feature.triprecording.data.AssignedVehicleRepositoryImpl
import org.vicpoo.gasolineproject.feature.triprecording.data.CostCenterRepositoryImpl
import org.vicpoo.gasolineproject.feature.triprecording.data.FuelRecordRepositoryImpl
import org.vicpoo.gasolineproject.feature.triprecording.data.GpsTripTracker
import org.vicpoo.gasolineproject.feature.triprecording.data.local.AssignedVehiclesSnapshot
import org.vicpoo.gasolineproject.feature.triprecording.data.local.CostCentersSnapshot
import org.vicpoo.gasolineproject.feature.triprecording.data.local.RecordingSnapshot
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.KtorTripRecordingApi
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.TripRecordingApi
import org.vicpoo.gasolineproject.feature.triprecording.data.sync.TripRecordingSyncer
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicleRepository
import org.vicpoo.gasolineproject.feature.triprecording.domain.CheckLocationPermissionUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.LocationPermissionChangedUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.SyncPendingTripsUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenterRepository
import org.vicpoo.gasolineproject.feature.triprecording.domain.DeleteFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.FinishTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.FinishTripValidator
import org.vicpoo.gasolineproject.feature.triprecording.domain.PauseTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ResumeTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RetryFinishTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.StopTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.UndoStopTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.presentation.finish.FinishTripViewModel
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecordRepository
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecordValidator
import org.vicpoo.gasolineproject.feature.triprecording.domain.GetAssignedVehicleUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.GetFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveCostCentersUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveFuelRecordsUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RefreshAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RefreshCostCentersUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RetryFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.SaveFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.StartTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripRecordingSync
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripTracker
import org.vicpoo.gasolineproject.feature.triprecording.presentation.active.ActiveTripViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.assigned.AssignedVehiclesViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.fuel.FuelRecordFormArgs
import org.vicpoo.gasolineproject.feature.triprecording.presentation.fuel.FuelRecordFormViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.start.StartTripArgs
import org.vicpoo.gasolineproject.feature.triprecording.presentation.start.StartTripViewModel

private val ASSIGNED_CACHE = named("assignedVehiclesCache")
private val COST_CENTERS_CACHE = named("costCentersCache")
private val RECORDING_CACHE = named("tripRecordingCache")

val tripRecordingModule = module {
    // ---------------- Data ----------------
    single<TripRecordingApi> { KtorTripRecordingApi(get()) }

    single(ASSIGNED_CACHE) {
        JsonFileCache(get(), get(), "assigned_vehicles.json", AssignedVehiclesSnapshot.serializer(), get(), AssignedVehiclesSnapshot())
    }
    single(COST_CENTERS_CACHE) {
        JsonFileCache(get(), get(), "cost_centers.json", CostCentersSnapshot.serializer(), get(), CostCentersSnapshot())
    }
    single(RECORDING_CACHE) {
        JsonFileCache(get(), get(), "trip_recording.json", RecordingSnapshot.serializer(), get(), RecordingSnapshot())
    }
    single {
        TripRecordingLocalStore(
            fileSystem = get(),
            directories = get(),
            json = get(),
            snapshotCache = get(RECORDING_CACHE),
        )
    }

    single { AssignedVehicleRepositoryImpl(api = get(), cache = get(ASSIGNED_CACHE), sessionReader = get()) }
    single<AssignedVehicleRepository> { get<AssignedVehicleRepositoryImpl>() }

    single { CostCenterRepositoryImpl(api = get(), cache = get(COST_CENTERS_CACHE), sessionReader = get()) }
    single<CostCenterRepository> { get<CostCenterRepositoryImpl>() }

    single<FuelRecordRepository> { FuelRecordRepositoryImpl(store = get(), clock = get()) }

    single<TripRecordingSync> { TripRecordingSyncer(store = get(), api = get(), appScope = get()) }

    // Se crea al iniciar la app: si quedó un viaje en curso, el GPS se reanuda solo
    single(createdAtStart = true) {
        GpsTripTracker(
            store = get(),
            locationProvider = get(),
            trackingService = get(),
            sync = get(),
            sessionReader = get(),
            clock = get(),
            appScope = get(),
        )
    }
    single<TripTracker> { get<GpsTripTracker>() }

    // Al cerrar sesión: se detiene el GPS y se borra todo lo guardado
    single<LogoutListener>(named("tripRecording")) {
        val tracker = get<GpsTripTracker>()
        val store = get<TripRecordingLocalStore>()
        val assigned = get<AssignedVehicleRepositoryImpl>()
        val costCenters = get<CostCenterRepositoryImpl>()
        LogoutListener {
            tracker.reset()
            store.clearAll()
            assigned.clear()
            costCenters.clear()
        }
    }

    // ---------------- Domain ----------------
    factoryOf(::FuelRecordValidator)
    factoryOf(::ObserveAssignedVehiclesUseCase)
    factoryOf(::RefreshAssignedVehiclesUseCase)
    factoryOf(::GetAssignedVehicleUseCase)
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
    factoryOf(::SaveFuelRecordUseCase)
    factoryOf(::RetryFuelRecordUseCase)
    factoryOf(::DeleteFuelRecordUseCase)
    factoryOf(::CheckLocationPermissionUseCase)
    factoryOf(::LocationPermissionChangedUseCase)
    factoryOf(::SyncPendingTripsUseCase)

    // ---------------- Presentation ----------------
    viewModelOf(::AssignedVehiclesViewModel)
    viewModelOf(::ActiveTripViewModel)
    viewModelOf(::FinishTripViewModel)
    viewModel { params ->
        StartTripViewModel(
            args = params.get<StartTripArgs>(),
            getVehicle = get(),
            observeCostCenters = get(),
            refreshCostCenters = get(),
            startTrip = get(),
            hasLocationPermission = get(),
            locationPermissionChanged = get(),
        )
    }
    viewModel { params ->
        FuelRecordFormViewModel(
            args = params.get<FuelRecordFormArgs>(),
            getRecord = get(),
            observeCostCenters = get(),
            observeTracking = get(),
            saveRecord = get(),
            retryRecord = get(),
        )
    }
}
