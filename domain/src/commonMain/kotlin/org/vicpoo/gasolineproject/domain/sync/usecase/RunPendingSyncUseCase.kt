package org.vicpoo.gasolineproject.domain.sync.usecase

import kotlinx.coroutines.withContext
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.sync.SyncRunOutcome
import org.vicpoo.gasolineproject.domain.sync.TemporarySyncFailures
import org.vicpoo.gasolineproject.domain.sync.usecase.SyncAllPendingUseCase

/** Una vuelta de sincronización en segundo plano y la decisión de reintento (la usa el Worker). */
class RunPendingSyncUseCase(private val syncAll: SyncAllPendingUseCase) {
    suspend operator fun invoke(): SyncRunOutcome {
        val failures = TemporarySyncFailures()
        val result = withContext(failures) { syncAll() }
        return when {
            result is AppResult.Failure && result.error == AppError.NoConnection -> SyncRunOutcome.RetryWithBackoff
            // La sesión terminó: no tiene caso reintentar hasta que vuelva a entrar
            result is AppResult.Failure && result.error == AppError.Unauthorized -> SyncRunOutcome.Done
            failures.count > 0 -> failures.maxRetryAfterMillis?.let { SyncRunOutcome.RetryAfter(it) }
                ?: SyncRunOutcome.RetryWithBackoff
            else -> SyncRunOutcome.Done
        }
    }
}
