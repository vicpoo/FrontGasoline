package org.vicpoo.gasolineproject.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.local.LocalDocumentCache
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
import org.vicpoo.gasolineproject.domain.session.OpenTripCheck
import org.vicpoo.gasolineproject.domain.session.OpenTripSummary
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
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
        LocalDocumentCache(get(), get(), get(), "assigned_vehicles.json", AssignedVehiclesSnapshot.serializer(), get(), AssignedVehiclesSnapshot())
    }
    single(COST_CENTERS_CACHE) {
        LocalDocumentCache(get(), get(), get(), "cost_centers.json", CostCentersSnapshot.serializer(), get(), CostCentersSnapshot())
    }
    single(RECORDING_CACHE) {
        LocalDocumentCache(get(), get(), get(), "trip_recording.json", RecordingSnapshot.serializer(), get(), RecordingSnapshot())
    }
    single {
        TripRecordingLocalStore(
            fileSystem = get(),
            directories = get(),
            json = get(),
            snapshotCache = get(RECORDING_CACHE),
            database = get(),
            fileCipher = get(),
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
        LocalDocumentCache(get(), get(), get(), "trip_sync_log.json", SyncLogSnapshot.serializer(), get(), SyncLogSnapshot())
    }
    single { TripSyncLog(cache = get(SYNC_LOG_CACHE), clock = get()) }

    // Una sola instancia: la usan los casos de uso (TripRecordingSync) y el trabajo en segundo plano (PendingSyncTask)
    single {
        TripRecordingSyncer(store = get(), api = get(), appScope = get(), scheduler = get(), log = get(), sessionReader = get())
    } binds arrayOf(TripRecordingSync::class, PendingSyncTask::class)

    singleOf(::SyncStatusRepositoryImpl) { bind<SyncStatusRepository>() }

    // Se crea al iniciar la app: si quedó un viaje en curso, el GPS se reanuda solo
    singleOf(::GpsTripTracker) {
        createdAtStart()
        bind<TripTracker>()
    }

    // Al cerrar sesión (o al entrar otra cuenta): el viaje abierto queda en pausa y se apaga el GPS.
    // Los viajes NO se borran (en curso o sin enviar): se guardan por cuenta y su dueño los retoma
    // al volver a entrar. Antes se borraban y en la API se quedaban "en curso" para siempre.
    // Lo demás (carros asignados, centros de costo, bitácora) se vuelve a descargar al entrar.
    single<LogoutListener>(named("tripRecording")) {
        val tracker = get<GpsTripTracker>()
        val assigned = get<AssignedVehicleRepositoryImpl>()
        val costCenters = get<CostCenterRepositoryImpl>()
        val syncLog = get<TripSyncLog>()
        LogoutListener {
            tracker.onSessionEnded()
            assigned.clear()
            costCenters.clear()
            syncLog.clear()
        }
    }

    // Perfil pregunta si hay un viaje abierto antes de "Salir" (sin depender de esta feature)
    single<OpenTripCheck> {
        val tracker = get<TripTracker>()
        OpenTripCheck {
            tracker.session.value.trip?.let { trip ->
                OpenTripSummary(
                    vehicleTitle = trip.vehicle.title,
                    plates = trip.vehicle.plates,
                    waitingForFinish = trip.phase == TripPhase.STOPPED,
                )
            }
        }
    }
}
