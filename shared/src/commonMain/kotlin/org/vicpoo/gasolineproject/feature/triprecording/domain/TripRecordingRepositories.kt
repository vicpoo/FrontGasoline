package org.vicpoo.gasolineproject.feature.triprecording.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.platform.PickedFile

/*
 * Contratos del dominio (Dependency Inversion): los casos de uso y ViewModels solo conocen
 * estas interfaces. La capa data decide cómo se guardan (archivos JSON) y cómo se envían (Ktor).
 */

/** Carros con reserva aprobada del usuario (lista offline + refresco desde la API). */
interface AssignedVehicleRepository {
    fun observe(): Flow<List<AssignedVehicle>>
    suspend fun refresh(): AppResult<Unit>
    suspend fun find(reservationId: String): AssignedVehicle?
}

/** Centros de costo activos de la empresa (lista offline + refresco desde la API). */
interface CostCenterRepository {
    fun observe(): Flow<List<CostCenter>>
    suspend fun refresh(): AppResult<Unit>
}

/**
 * Registro GPS del viaje en curso. Vive mientras la app esté abierta (no depende de una pantalla)
 * y se reanuda solo si la app se cerró con un viaje sin terminar.
 */
interface TripTracker {
    val session: StateFlow<TrackingSession>

    /** Inicia el viaje: se guarda primero en el teléfono y después se envía a la API. */
    suspend fun start(vehicle: AssignedVehicle, costCenter: CostCenter?): AppResult<RecordedTrip>

    /** Pausa: se apaga el GPS (la distancia y la duración no cuentan mientras está en pausa). */
    suspend fun pause(): AppResult<RecordedTrip>

    /** Reanuda: se vuelve a encender el GPS. */
    suspend fun resume(): AppResult<RecordedTrip>

    /** Botón ■ + "Sí": se apaga el GPS y el viaje espera el kilometraje final y la foto. */
    suspend fun stop(): AppResult<RecordedTrip>

    /** Atrás en "Registrar Viaje": el viaje sigue como estaba (en curso o en pausa). */
    suspend fun undoStop(): AppResult<RecordedTrip>

    /**
     * Guarda "Registrar Viaje" en el teléfono (kilometraje final + foto del odómetro) y lo envía.
     * Si el servidor lo rechaza, el viaje vuelve a quedar detenido para corregir el kilometraje.
     */
    suspend fun finish(finalMileage: Double, odometerPhoto: PickedFile): AppResult<SendOutcome>

    /** "Reintentar" después de guardar sin conexión. */
    suspend fun retryFinishSend(): SendOutcome

    /** Vuelve a revisar el permiso de ubicación (después de pedirlo) y reanuda el GPS. */
    fun onPermissionChanged()
}

/** Valores ya validados de una recarga. */
data class FuelRecordValues(
    val amount: Double?,
    val liters: Double,
    val costCenter: CostCenter?,
)

/** Recargas de gasolina del viaje (offline-first). */
interface FuelRecordRepository {
    fun observeForTrip(tripLocalId: String): Flow<List<FuelRecord>>
    suspend fun get(localId: String): FuelRecord?

    /** Crea (localId == null) o edita una recarga en el teléfono. */
    suspend fun save(
        tripLocalId: String,
        localId: String?,
        values: FuelRecordValues,
        newReceipt: PickedFile?,
    ): AppResult<FuelRecord>

    /** Marca la recarga para borrarse (si nunca se envió, se borra al momento). */
    suspend fun delete(localId: String): AppResult<Unit>
}

/** Envía a la API todo lo pendiente: viajes, puntos GPS, recargas y cierres. */
interface TripRecordingSync {
    suspend fun sync(): AppResult<Unit>

    /** Lanza la sincronización en segundo plano (no espera el resultado). */
    fun requestSync()
}
