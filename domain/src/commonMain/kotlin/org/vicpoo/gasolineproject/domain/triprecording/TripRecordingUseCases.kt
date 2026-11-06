package org.vicpoo.gasolineproject.domain.triprecording

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile

// ---------------- Carros asignados y centros de costo ----------------

class ObserveAssignedVehiclesUseCase(private val repository: AssignedVehicleRepository) {
    operator fun invoke(): Flow<List<AssignedVehicle>> = repository.observe()
}

class RefreshAssignedVehiclesUseCase(private val repository: AssignedVehicleRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}

/** Qué carros no se pueden dejar todavía y por qué (viaje en curso o sin enviar). */
class ObserveReleaseBlocksUseCase(private val repository: AssignedVehicleRepository) {
    operator fun invoke(): Flow<Map<String, ReleaseBlock>> = repository.observeReleaseBlocks()
}

/**
 * "Dejar carro": el empleado ya no lo necesita y queda disponible para otros.
 * Se revisa primero en el teléfono (viaje en curso o sin enviar); la API lo vuelve a validar.
 * Las observaciones son opcionales (máx. 500).
 */
class ReleaseVehicleUseCase(private val repository: AssignedVehicleRepository) {
    suspend operator fun invoke(reservationId: String, notes: String): AppResult<Unit> {
        val clean = notes.trim().takeIf { it.isNotEmpty() }
        if (clean != null && clean.length > MAX_NOTES) return AppResult.Failure(AppError.Validation(ValidationReason.OUT_OF_RANGE))
        // Mismo código que la API (409); la pantalla muestra el motivo con ReleaseBlock
        if (repository.observeReleaseBlocks().first().containsKey(reservationId)) {
            return AppResult.Failure(AppError.Server(409, null))
        }
        return repository.release(reservationId, clean)
    }

    companion object {
        const val MAX_NOTES = 500
    }
}

class GetAssignedVehicleUseCase(private val repository: AssignedVehicleRepository) {
    suspend operator fun invoke(reservationId: String): AssignedVehicle? = repository.find(reservationId)
}

class ObserveCostCentersUseCase(private val repository: CostCenterRepository) {
    operator fun invoke(): Flow<List<CostCenter>> = repository.observe()
}

class RefreshCostCentersUseCase(private val repository: CostCenterRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}

// ---------------- Viaje en curso ----------------

class ObserveTrackingUseCase(private val tracker: TripTracker) {
    operator fun invoke(): StateFlow<TrackingSession> = tracker.session
}

/**
 * Inicia un viaje con el carro asignado.
 * Reglas: un solo viaje a la vez y, si la empresa tiene centros de costo, elegir uno.
 */
class StartTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(
        vehicle: AssignedVehicle,
        costCenter: CostCenter?,
        availableCostCenters: List<CostCenter>,
    ): AppResult<RecordedTrip> {
        if (availableCostCenters.isNotEmpty() && costCenter == null) {
            return AppResult.Failure(AppError.Validation(ValidationReason.REQUIRED))
        }
        tracker.session.value.trip?.let { running -> return AppResult.Success(running) }
        return tracker.start(vehicle, costCenter)
    }
}

class PauseTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(): AppResult<RecordedTrip> = tracker.pause()
}

class ResumeTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(): AppResult<RecordedTrip> = tracker.resume()
}

/** "¿Está seguro de terminar con el viaje?" → Sí. */
class StopTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(): AppResult<RecordedTrip> = tracker.stop()
}

/** Regresar desde "Registrar Viaje" sin guardar: el viaje continúa. */
class UndoStopTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(): AppResult<RecordedTrip> = tracker.undoStop()
}

/** Resultado de guardar "Registrar Viaje". */
sealed interface FinishTripResult {
    data class Invalid(val errors: Map<FinishTripField, ValidationReason>) : FinishTripResult
    data class Saved(val outcome: SendOutcome) : FinishTripResult
    data class Failed(val error: AppError) : FinishTripResult
}

/**
 * Termina el viaje con el kilometraje final y la foto del odómetro (offline-first: primero se guarda
 * en el teléfono y después se intenta enviar). Así la pantalla puede mostrar "¡Viaje registrado!"
 * o "¡Error al enviar!" sin perder nada.
 */
class FinishTripUseCase(
    private val tracker: TripTracker,
    private val validator: FinishTripValidator,
    private val imageCompressor: ImageCompressor,
) {
    /**
     * NonCancellable: si el usuario sale de la pantalla mientras se comprime la foto o se guarda,
     * el viaje igual queda guardado (antes se podía perder el cierre al salir a medio guardar).
     */
    suspend operator fun invoke(finalMileageText: String, odometerPhoto: PickedFile?): FinishTripResult =
        withContext(NonCancellable) { finish(finalMileageText, odometerPhoto) }

    private suspend fun finish(finalMileageText: String, odometerPhoto: PickedFile?): FinishTripResult {
        val trip = tracker.session.value.trip ?: return FinishTripResult.Failed(AppError.NotFound)

        val photo = odometerPhoto?.let { imageCompressor.compress(it) }
        val mileage = when (val validation = validator.validate(finalMileageText, trip.initialMileage, photo)) {
            is FinishTripValidation.Invalid -> return FinishTripResult.Invalid(validation.errors)
            is FinishTripValidation.Valid -> validation.finalMileage
        }

        return when (val result = tracker.finish(mileage, photo!!)) {
            is AppResult.Success -> FinishTripResult.Saved(result.data)
            is AppResult.Failure -> FinishTripResult.Failed(result.error)
        }
    }
}

/** "Reintentar" en el diálogo de error: vuelve a intentar enviar el viaje guardado. */
class RetryFinishTripUseCase(private val tracker: TripTracker) {
    suspend operator fun invoke(): SendOutcome = tracker.retryFinishSend()
}

/** ¿Ya hay permiso de ubicación? (antes de iniciar el viaje se pide si no). */
class CheckLocationPermissionUseCase(private val locationProvider: LocationProvider) {
    operator fun invoke(): Boolean = locationProvider.hasPermission()
}

/** El usuario respondió al permiso de ubicación: el tracker reanuda el GPS si hace falta. */
class LocationPermissionChangedUseCase(private val tracker: TripTracker) {
    operator fun invoke() = tracker.onPermissionChanged()
}

/** Manda en segundo plano lo que haya quedado pendiente (viajes, puntos, recargas). */
class SyncPendingTripsUseCase(private val sync: TripRecordingSync) {
    operator fun invoke() = sync.requestSync()
}

// ---------------- Recargas de gasolina ----------------

class ObserveFuelRecordsUseCase(private val repository: FuelRecordRepository) {
    operator fun invoke(tripLocalId: String): Flow<List<FuelRecord>> = repository.observeForTrip(tripLocalId)
}

class GetFuelRecordUseCase(private val repository: FuelRecordRepository) {
    suspend operator fun invoke(localId: String): FuelRecord? = repository.get(localId)
}

/** Resultado de guardar el formulario de recarga. */
sealed interface SaveFuelResult {
    data class Invalid(val errors: Map<FuelField, ValidationReason>) : SaveFuelResult
    data class Saved(val recordId: String, val outcome: SendOutcome) : SaveFuelResult
    data class Failed(val error: AppError) : SaveFuelResult
}

/**
 * Punto donde se está haciendo la carga de gasolina. Funciona sin internet (el GPS no usa red).
 * Orden de preferencia:
 * 1. La lectura actual del GPS del viaje, si es reciente (aunque el filtro de ruta la haya
 *    descartado por estar detenido: justo es el caso de la gasolinera).
 * 2. La última ubicación del teléfono, si tiene menos de [MAX_LAST_KNOWN_AGE_MS] y buena precisión.
 * 3. La última lectura del GPS del viaje (aunque no sea reciente).
 * 4. El último punto aceptado de la ruta.
 * null = no hay nada confiable; la API la completa con el punto de la ruta más cercano a la hora.
 */
class CaptureFuelLocationUseCase(
    private val tracker: TripTracker,
    private val locationProvider: LocationProvider,
    private val clock: AppClock,
) {
    suspend operator fun invoke(): GeoPoint? {
        val session = tracker.session.value
        if (session.gpsActive && session.current != null) return session.current

        val lastKnown = runCatching { locationProvider.lastKnown() }.getOrNull()
        if (lastKnown != null &&
            clock.nowMillis() - lastKnown.timeMillis <= MAX_LAST_KNOWN_AGE_MS &&
            (lastKnown.accuracyMeters ?: 0.0) <= MAX_ACCURACY_M
        ) {
            return lastKnown.point
        }
        return session.current ?: session.trip?.lastLocation
    }

    private companion object {
        const val MAX_LAST_KNOWN_AGE_MS = 2 * 60_000L
        const val MAX_ACCURACY_M = 50.0
    }
}

/**
 * Guarda la recarga en el teléfono (offline-first) y luego intenta enviarla.
 * - Guardar es NonCancellable: aunque el usuario salga de la pantalla, la recarga queda guardada.
 * - El envío corre en segundo plano (scope de la app) y aquí solo se espera unos segundos
 *   ([SEND_WAIT_MS]); con señal débil sigue enviándose solo y la pantalla dice "guardada".
 */
class SaveFuelRecordUseCase(
    private val repository: FuelRecordRepository,
    private val sync: TripRecordingSync,
    private val tracker: TripTracker,
    private val costCenters: CostCenterRepository,
    private val validator: FuelRecordValidator,
    private val imageCompressor: ImageCompressor,
    private val captureLocation: CaptureFuelLocationUseCase,
) {
    suspend operator fun invoke(draft: FuelRecordDraft): SaveFuelResult =
        withContext(NonCancellable) { save(draft) }

    private suspend fun save(draft: FuelRecordDraft): SaveFuelResult {
        val trip = tracker.session.value.trip
            ?: return SaveFuelResult.Failed(AppError.NotFound)

        val receipt = draft.newReceipt?.let { imageCompressor.compress(it) }
        val prepared = draft.copy(newReceipt = receipt)
        val values = when (
            val validation = validator.validate(prepared, costCenters.observe().first(), trip.vehicle.tankCapacityLiters)
        ) {
            is FuelValidation.Invalid -> return SaveFuelResult.Invalid(validation.errors)
            is FuelValidation.Valid -> validation.values
        }

        // Solo al crear: la ubicación de una carga ya registrada no se mueve al editarla
        val location = if (draft.localId == null) draft.location ?: captureLocation() else null

        val saved = when (val result = repository.save(trip.localId, draft.localId, values, receipt, location)) {
            is AppResult.Failure -> return SaveFuelResult.Failed(result.error)
            is AppResult.Success -> result.data
        }
        return SaveFuelResult.Saved(recordId = saved.localId, outcome = sendAndReport(saved.localId))
    }

    private suspend fun sendAndReport(localId: String): SendOutcome =
        sync.outcomeFor(localId) { repository.get(it) }
}

/** Reintenta enviar una recarga que quedó pendiente o rechazada (botón "Reintentar"). */
class RetryFuelRecordUseCase(
    private val repository: FuelRecordRepository,
    private val sync: TripRecordingSync,
) {
    suspend operator fun invoke(localId: String): SendOutcome {
        repository.markForRetry(localId)
        return sync.outcomeFor(localId) { repository.get(it) }
    }
}

class DeleteFuelRecordUseCase(
    private val repository: FuelRecordRepository,
    private val sync: TripRecordingSync,
) {
    suspend operator fun invoke(localId: String): AppResult<Unit> {
        val result = repository.delete(localId)
        if (result is AppResult.Success) sync.requestSync()
        return result
    }
}

/**
 * Cuánto se espera el envío antes de responderle al usuario. Con buena señal una recarga llega en
 * 1–3 s; si tarda más, se le dice que quedó guardada y el envío sigue en segundo plano.
 */
const val SEND_WAIT_MS = 8_000L

/** Sincroniza (esperando como máximo [SEND_WAIT_MS]) y traduce el estado de la recarga a un SendOutcome. */
private suspend fun TripRecordingSync.outcomeFor(
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
