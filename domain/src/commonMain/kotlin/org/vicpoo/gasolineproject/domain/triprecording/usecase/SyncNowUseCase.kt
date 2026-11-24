package org.vicpoo.gasolineproject.domain.triprecording.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.sync.usecase.SyncAllPendingUseCase

/**
 * "Enviar ahora": intenta mandar todo lo pendiente de TODA la app (viajes y recargas primero, luego
 * solicitudes y vehículos). Espera como máximo [waitMillis]; si con señal débil tarda más, el envío
 * SIGUE en el scope de la app aunque se salga de la pantalla (regresa null).
 */
class SyncNowUseCase(
    private val syncAll: SyncAllPendingUseCase,
    private val appScope: AppScope,
) {
    suspend operator fun invoke(waitMillis: Long = DEFAULT_WAIT_MS): AppResult<Unit>? {
        val running = appScope.scope.async { syncAll() }
        return withTimeoutOrNull(waitMillis) { running.await() }
    }

    private companion object {
        const val DEFAULT_WAIT_MS = 60_000L
    }
}
