package org.vicpoo.gasolineproject.core.sync

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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import java.util.concurrent.TimeUnit

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

    override fun cancel() {
        workManager.cancelUniqueWork(PERIODIC_WORK)
        workManager.cancelUniqueWork(ONE_TIME_WORK)
    }

    private companion object {
        const val PERIODIC_WORK = "gasoline.sync.periodic"
        const val ONE_TIME_WORK = "gasoline.sync.when-online"
        const val PERIODIC_MINUTES = 15L // mínimo que permite WorkManager
        const val BACKOFF_SECONDS = 30L
    }
}

/**
 * Trabajo de WorkManager: solo llama al caso de uso (la lógica vive en commonMain).
 * Sin red → retry (WorkManager lo vuelve a lanzar con backoff). Cualquier otro caso termina:
 * los rechazos de la API ya quedaron guardados en cada registro.
 */
class PendingSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val syncAll: SyncAllPendingUseCase by inject()

    override suspend fun doWork(): Result = when (val result = syncAll()) {
        is AppResult.Success -> Result.success()
        is AppResult.Failure -> if (result.error == AppError.NoConnection) Result.retry() else Result.success()
    }
}
