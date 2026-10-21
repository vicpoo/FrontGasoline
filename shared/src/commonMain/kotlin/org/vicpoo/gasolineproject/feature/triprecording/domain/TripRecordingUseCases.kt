package org.vicpoo.gasolineproject.feature.triprecording.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.location.LocationProvider
import org.vicpoo.gasolineproject.core.platform.ImageCompressor
import org.vicpoo.gasolineproject.core.platform.PickedFile

// ---------------- Carros asignados y centros de costo ----------------

class ObserveAssignedVehiclesUseCase(private val repository: AssignedVehicleRepository) {
    operator fun invoke(): Flow<List<AssignedVehicle>> = repository.observe()
}

class RefreshAssignedVehiclesUseCase(private val repository: AssignedVehicleRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
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
    suspend operator fun invoke(finalMileageText: String, odometerPhoto: PickedFile?): FinishTripResult {
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
 * Guarda la recarga en el teléfono (offline-first) y luego intenta enviarla de inmediato.
 * Así la pantalla puede mostrar "¡Solicitud enviada!" o "¡Error al enviar!" sin perder el registro.
 */
class SaveFuelRecordUseCase(
    private val repository: FuelRecordRepository,
    private val sync: TripRecordingSync,
    private val tracker: TripTracker,
    private val costCenters: CostCenterRepository,
    private val validator: FuelRecordValidator,
    private val imageCompressor: ImageCompressor,
) {
    suspend operator fun invoke(draft: FuelRecordDraft): SaveFuelResult {
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

        val saved = when (val result = repository.save(trip.localId, draft.localId, values, receipt)) {
            is AppResult.Failure -> return SaveFuelResult.Failed(result.error)
            is AppResult.Success -> result.data
        }
        return SaveFuelResult.Saved(recordId = saved.localId, outcome = sendAndReport(saved.localId))
    }

    private suspend fun sendAndReport(localId: String): SendOutcome =
        sync.outcomeFor(localId) { repository.get(it) }
}

/** Reintenta enviar una recarga que quedó pendiente (botón "Reintentar"). */
class RetryFuelRecordUseCase(
    private val repository: FuelRecordRepository,
    private val sync: TripRecordingSync,
) {
    suspend operator fun invoke(localId: String): SendOutcome = sync.outcomeFor(localId) { repository.get(it) }
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

/** Sincroniza y traduce el estado final de la recarga a un SendOutcome. */
private suspend fun TripRecordingSync.outcomeFor(
    localId: String,
    find: suspend (String) -> FuelRecord?,
): SendOutcome {
    val syncResult = sync()
    val record = find(localId)
    return when {
        record == null || record.syncState == RecordSyncState.SYNCED -> SendOutcome.Sent
        record.syncState == RecordSyncState.ERROR -> SendOutcome.Rejected(record.syncErrorMessage)
        syncResult is AppResult.Failure && syncResult.error != AppError.NoConnection ->
            SendOutcome.Rejected(null)
        else -> SendOutcome.SavedOffline
    }
}
