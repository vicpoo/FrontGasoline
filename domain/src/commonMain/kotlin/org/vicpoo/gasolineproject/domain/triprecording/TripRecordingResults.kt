package org.vicpoo.gasolineproject.domain.triprecording

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason

// ---------------- Viaje en curso ----------------

/** Resultado de guardar "Registrar Viaje". */
sealed interface FinishTripResult {
    data class Invalid(val errors: Map<FinishTripField, ValidationReason>) : FinishTripResult
    data class Saved(val outcome: SendOutcome) : FinishTripResult
    data class Failed(val error: AppError) : FinishTripResult
}

// ---------------- Recargas de gasolina ----------------

/** Resultado de guardar el formulario de recarga. */
sealed interface SaveFuelResult {
    data class Invalid(val errors: Map<FuelField, ValidationReason>) : SaveFuelResult
    data class Saved(val recordId: String, val outcome: SendOutcome) : SaveFuelResult
    data class Failed(val error: AppError) : SaveFuelResult
}

/**
 * Cuánto se espera el envío antes de responderle al usuario. Con buena señal una recarga llega en
 * 1–3 s; si tarda más, se le dice que quedó guardada y el envío sigue en segundo plano.
 */
const val SEND_WAIT_MS = 8_000L

/** Sincroniza (esperando como máximo [SEND_WAIT_MS]) y traduce el estado de la recarga a un SendOutcome. */
internal suspend fun TripRecordingSync.outcomeFor(
    localId: String,
    find: suspend (String) -> FuelRecord?,
): SendOutcome {
    val syncResult = syncWithin(SEND_WAIT_MS)
    val record = find(localId)
    return when {
        record == null || record.syncState == RecordSyncState.SYNCED -> SendOutcome.Sent
        record.syncState == RecordSyncState.ERROR -> SendOutcome.Rejected(record.syncErrorMessage)
        // Sigue enviándose en segundo plano (señal débil): quedó guardada
        syncResult == null -> SendOutcome.SavedOffline
        // "Sin conexión" SOLO si de verdad falló la red; si no, se dice que hubo un error
        // (ej. la API rechazó el viaje y por eso la recarga no se pudo mandar)
        syncResult is AppResult.Failure && syncResult.error == AppError.NoConnection -> SendOutcome.SavedOffline
        else -> SendOutcome.Rejected(record.syncErrorMessage)
    }
}
