package org.vicpoo.gasolineproject.domain.di

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppLogger
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.common.SystemAppClock
import org.vicpoo.gasolineproject.domain.common.e
import org.vicpoo.gasolineproject.domain.push.PushEvents
import org.vicpoo.gasolineproject.domain.rbac.PermissionChecker
import org.vicpoo.gasolineproject.domain.rbac.SessionPermissionChecker
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncCoordinator
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.domain.sync.usecase.CountLostOnLogoutUseCase
import org.vicpoo.gasolineproject.domain.sync.usecase.CountPendingUseCase
import org.vicpoo.gasolineproject.domain.sync.usecase.ObservePendingSummaryUseCase
import org.vicpoo.gasolineproject.domain.sync.usecase.PendingBeforeLogoutUseCase
import org.vicpoo.gasolineproject.domain.sync.usecase.RunPendingSyncUseCase
import org.vicpoo.gasolineproject.domain.sync.usecase.SyncAllPendingUseCase

/** Piezas compartidas: scope de la app, reloj, privilegios, push y sincronización offline-first. */
val commonDomainModule = module {
    // Scope de la app: una excepción no capturada en un launch ya no se pierde, queda en la bitácora
    single {
        val logger = get<AppLogger>()
        val onError = CoroutineExceptionHandler { _, error -> logger.e("AppScope", "Error no capturado en segundo plano", error) }
        AppScope(CoroutineScope(SupervisorJob() + Dispatchers.Default + onError))
    }
    single<AppClock> { SystemAppClock() }
    single<PermissionChecker> { SessionPermissionChecker(get()) }
    single { PushEvents() }

    // Todas las features registradas con `bind PendingSyncTask::class`
    factory { SyncAllPendingUseCase(getAll<PendingSyncTask>()) }
    // Una vuelta en segundo plano + si hay que reintentar (Worker de Android)
    factory { RunPendingSyncUseCase(get()) }
    factory { PendingBeforeLogoutUseCase(getAll<PendingSyncTask>()) }
    factory { CountPendingUseCase(getAll<PendingSyncTask>()) }
    factory { CountLostOnLogoutUseCase(getAll<PendingSyncTask>()) }
    factory { ObservePendingSummaryUseCase(getAll<PendingSyncTask>()) }
    single(createdAtStart = true) {
        BackgroundSyncCoordinator(sessionReader = get(), scheduler = get(), connectivity = get(), appScope = get())
    }
}
