package org.vicpoo.gasolineproject.feature.triprecording.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.core.sync.PendingSyncTask
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
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenterRepository
import org.vicpoo.gasolineproject.feature.triprecording.domain.DeleteFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.FinishTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.FinishTripValidator
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecordRepository
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecordValidator
import org.vicpoo.gasolineproject.feature.triprecording.domain.GetAssignedVehicleUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.GetFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.LocationPermissionChangedUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveCostCentersUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveFuelRecordsUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.PauseTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RefreshAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RefreshCostCentersUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ResumeTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RetryFinishTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RetryFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.SaveFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.StartTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.StopTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.SyncPendingTripsUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripRecordingSync
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripTracker
import org.vicpoo.gasolineproject.feature.triprecording.domain.UndoStopTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.presentation.active.ActiveTripViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.assigned.AssignedVehiclesViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.finish.FinishTripViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.fuel.FuelRecordFormViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.start.StartTripViewModel

private val ASSIGNED_CACHE = named("assignedVehiclesCache")
private val COST_CENTERS_CACHE = named("costCentersCache")
private val RECORDING_CACHE = named("tripRecordingCache")

val tripRecordingModule = module {
    // ---------------- Data ----------------
    singleOf(::KtorTripRecordingApi) { bind<TripRecordingApi>() }

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

    // Explícitos: reciben un caché con nombre (named), eso no se resuelve solo por tipo
    single { AssignedVehicleRepositoryImpl(api = get(), cache = get(ASSIGNED_CACHE), sessionReader = get()) }
    single<AssignedVehicleRepository> { get<AssignedVehicleRepositoryImpl>() }

    single { CostCenterRepositoryImpl(api = get(), cache = get(COST_CENTERS_CACHE), sessionReader = get()) }
    single<CostCenterRepository> { get<CostCenterRepositoryImpl>() }

    singleOf(::FuelRecordRepositoryImpl) { bind<FuelRecordRepository>() }

    // Una sola instancia: la usan los casos de uso (TripRecordingSync) y el trabajo en segundo plano (PendingSyncTask)
    singleOf(::TripRecordingSyncer) {
        bind<TripRecordingSync>()
        bind<PendingSyncTask>()
    }

    // Se crea al iniciar la app: si quedó un viaje en curso, el GPS se reanuda solo
    singleOf(::GpsTripTracker) {
        createdAtStart()
        bind<TripTracker>()
    }

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
    // StartTripArgs / FuelRecordFormArgs llegan con parametersOf y Koin los toma por tipo
    viewModelOf(::StartTripViewModel)
    viewModelOf(::FuelRecordFormViewModel)
}
