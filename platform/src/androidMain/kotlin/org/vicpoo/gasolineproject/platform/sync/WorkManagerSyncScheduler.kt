package org.vicpoo.gasolineproject.platform.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.domain.sync.SyncRunOutcome
import org.vicpoo.gasolineproject.domain.sync.usecase.RunPendingSyncUseCase

/**
 * Android: WorkManager. Los trabajos solo corren con red y sobreviven a que se cierre la app
 * o se reinicie el teléfono, así lo guardado sin conexión llega a la API en cuanto hay internet.
 */
class WorkManagerSyncScheduler(context: Context) : BackgroundSyncScheduler {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    private val onlyWithNetwork = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<PendingSyncWorker>(PERIODIC_MINUTES, TimeUnit.MINUTES)
            .setConstraints(onlyWithNetwork)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun requestWhenOnline() {
        val request = OneTimeWorkRequestBuilder<PendingSyncWorker>()
            .setConstraints(onlyWithNetwork)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        // KEEP: si ya hay uno esperando la red no se duplica
        workManager.enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.KEEP, request)
    }

    /** La API pidió esperar (Retry-After): una vuelta después de ese tiempo (entre 30 s y 1 h). */
    override fun requestAfter(delayMillis: Long) {
        val delay = delayMillis.coerceIn(MIN_RETRY_AFTER_MS, MAX_RETRY_AFTER_MS)
        val request = OneTimeWorkRequestBuilder<PendingSyncWorker>()
            .setConstraints(onlyWithNetwork)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        // Nombre propio: el trabajo que lo pide puede ser el mismo ONE_TIME_WORK que sigue corriendo
        workManager.enqueueUniqueWork(RETRY_AFTER_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(PERIODIC_WORK)
        workManager.cancelUniqueWork(ONE_TIME_WORK)
        workManager.cancelUniqueWork(RETRY_AFTER_WORK)
    }

    private companion object {
        const val PERIODIC_WORK = "gasoline.sync.periodic"
        const val ONE_TIME_WORK = "gasoline.sync.when-online"
        const val RETRY_AFTER_WORK = "gasoline.sync.retry-after"
        const val MIN_RETRY_AFTER_MS = 30_000L
        const val MAX_RETRY_AFTER_MS = 60 * 60_000L
        const val PERIODIC_MINUTES = 15L // mínimo que permite WorkManager
        const val BACKOFF_SECONDS = 30L
    }
}

/**
 * Trabajo de WorkManager: solo llama al caso de uso (la lógica vive en commonMain, ver SyncRetry.kt).
 * - Sin red o error temporal del servidor (5xx, 408, 429) → retry: WorkManager lo vuelve a lanzar con
 *   backoff exponencial (30 s, 1 min, 2 min...), hasta [MAX_ATTEMPTS]; después queda para la vuelta periódica.
 * - La API pidió esperar (Retry-After) → se programa otra vuelta después de ese tiempo.
 * - Rechazos definitivos (4xx): ya quedaron guardados en cada registro; no se reintenta solo.
 */
class PendingSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val runPendingSync: RunPendingSyncUseCase by inject()
    private val scheduler: BackgroundSyncScheduler by inject()

    override suspend fun doWork(): Result = when (val outcome = runPendingSync()) {
        SyncRunOutcome.Done -> Result.success()
        SyncRunOutcome.RetryWithBackoff -> if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        is SyncRunOutcome.RetryAfter -> {
            scheduler.requestAfter(outcome.delayMillis)
            Result.success()
        }
    }

    private companion object {
        /** 30 s + 1 + 2 + 4 + 8 min ≈ 15 min: a partir de ahí lo toma la vuelta periódica. */
        const val MAX_ATTEMPTS = 5
    }
}
