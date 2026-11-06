package org.vicpoo.gasolineproject.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.local.JsonFileCache
import org.vicpoo.gasolineproject.data.triprecording.AssignedVehicleRepositoryImpl
import org.vicpoo.gasolineproject.data.triprecording.CostCenterRepositoryImpl
import org.vicpoo.gasolineproject.data.triprecording.FuelRecordRepositoryImpl
import org.vicpoo.gasolineproject.data.triprecording.GpsTripTracker
import org.vicpoo.gasolineproject.data.triprecording.SyncStatusRepositoryImpl
import org.vicpoo.gasolineproject.data.triprecording.local.AssignedVehiclesSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.CostCentersSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.RecordingSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.SyncLogSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.data.triprecording.remote.KtorTripRecordingApi
import org.vicpoo.gasolineproject.data.triprecording.remote.TripRecordingApi
import org.vicpoo.gasolineproject.data.triprecording.sync.TripRecordingSyncer
import org.vicpoo.gasolineproject.data.triprecording.sync.TripSyncLog
import org.vicpoo.gasolineproject.domain.session.LogoutListener
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicleRepository
import org.vicpoo.gasolineproject.domain.triprecording.CostCenterRepository
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordRepository
import org.vicpoo.gasolineproject.domain.triprecording.SyncStatusRepository
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker

private val ASSIGNED_CACHE = named("assignedVehiclesCache")
private val COST_CENTERS_CACHE = named("costCentersCache")
private val RECORDING_CACHE = named("tripRecordingCache")
private val SYNC_LOG_CACHE = named("tripSyncLogCache")

val tripRecordingDataModule = module {
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
    single {
        AssignedVehicleRepositoryImpl(api = get(), cache = get(ASSIGNED_CACHE), sessionReader = get(), store = get(), clock = get())
    }
    single<AssignedVehicleRepository> { get<AssignedVehicleRepositoryImpl>() }

    single { CostCenterRepositoryImpl(api = get(), cache = get(COST_CENTERS_CACHE), sessionReader = get()) }
    single<CostCenterRepository> { get<CostCenterRepositoryImpl>() }

    singleOf(::FuelRecordRepositoryImpl) { bind<FuelRecordRepository>() }

    // Bitácora de los intentos de envío (pantalla "Envíos pendientes")
    single(SYNC_LOG_CACHE) {
        JsonFileCache(get(), get(), "trip_sync_log.json", SyncLogSnapshot.serializer(), get(), SyncLogSnapshot())
    }
    single { TripSyncLog(cache = get(SYNC_LOG_CACHE), clock = get()) }

    // Una sola instancia: la usan los casos de uso (TripRecordingSync) y el trabajo en segundo plano (PendingSyncTask)
    single {
        TripRecordingSyncer(store = get(), api = get(), appScope = get(), scheduler = get(), log = get())
    } binds arrayOf(TripRecordingSync::class, PendingSyncTask::class)

    singleOf(::SyncStatusRepositoryImpl) { bind<SyncStatusRepository>() }

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
        val syncLog = get<TripSyncLog>()
        LogoutListener {
            tracker.reset()
            store.clearAll()
            assigned.clear()
            costCenters.clear()
            syncLog.clear()
        }
    }
}
