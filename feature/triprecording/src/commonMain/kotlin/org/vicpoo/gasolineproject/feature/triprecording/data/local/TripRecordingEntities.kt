package org.vicpoo.gasolineproject.feature.triprecording.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Lo que se guarda en el teléfono (archivos JSON). Son clases separadas del dominio para que
 * cambiar el formato de almacenamiento no afecte a las pantallas ni a los casos de uso.
 */

@Serializable
data class AssignedVehicleEntity(
    val reservationId: String,
    val vehicleId: String,
    val brand: String,
    val model: String,
    val plates: String,
    val color: String? = null,
    val photoUrl: String? = null,
    val mileage: Double = 0.0,
    val tankCapacityLiters: Double? = null,
)

@Serializable
data class AssignedVehiclesSnapshot(
    val vehicles: List<AssignedVehicleEntity> = emptyList(),
)

@Serializable
data class CostCenterEntity(
    val id: String,
    val name: String,
    val key: String,
)

@Serializable
data class CostCentersSnapshot(
    val costCenters: List<CostCenterEntity> = emptyList(),
)

/** Viaje registrado en el teléfono (en curso o terminado pendiente de enviar). */
@Serializable
data class TripEntity(
    val localId: String,
    val serverId: String? = null,
    val companyId: String,
    val employeeId: String,
    val vehicle: AssignedVehicleEntity,
    val costCenter: CostCenterEntity? = null,
    val startedAt: String,
    val startedAtMillis: Long,
    val startLat: Double? = null,
    val startLng: Double? = null,
    /**
     * Hora en que el usuario detuvo el viaje (botón ■ → "Sí"). Con running = true y endedAt != null
     * el viaje está en la pantalla "Registrar Viaje" esperando el kilometraje final y la foto.
     */
    val endedAt: String? = null,
    val endedAtMillis: Long? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val initialMileage: Double,
    val distanceMeters: Double = 0.0,
    val lastLat: Double? = null,
    val lastLng: Double? = null,
    /** true hasta que el usuario guarda "Registrar Viaje" (en curso, en pausa o detenido). */
    val running: Boolean = true,
    /** Pausas del viaje en orden (la última puede seguir abierta). */
    val pauses: List<PauseEntity> = emptyList(),
    /** Kilometraje final que captura el usuario (odómetro). */
    val finalMileage: Double? = null,
    /** Foto del odómetro al terminar, guardada en el teléfono hasta subirla. */
    val odometerPhotoPath: String? = null,
    val odometerPhotoMimeType: String? = null,
    val odometerPhotoSent: Boolean = false,
    /** true cuando la API ya lo tiene como finalizado (finalizar o sincronizar). */
    val closedOnServer: Boolean = false,
    /** Cuántos puntos GPS del archivo ya se procesaron (enviados o descartados). */
    val pointsSent: Int = 0,
    /** Puntos que la API rechazó de forma definitiva (400): se saltan para no reintentarlos siempre. */
    val pointsDiscarded: Int = 0,
    val syncErrorMessage: String? = null,
    /** Código HTTP del último rechazo (4xx = definitivo, no se reintenta solo). */
    val syncErrorCode: Int? = null,
)

/** Pausa del viaje guardada en el teléfono. */
@Serializable
data class PauseEntity(
    val localId: String,
    val startedAt: String,
    val startedAtMillis: Long,
    val endedAt: String? = null,
    val endedAtMillis: Long? = null,
    val reason: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    /** POST /viajes/{id}/pausar ya llegó a la API. */
    val startSent: Boolean = false,
    /** POST /viajes/{id}/reanudar ya llegó a la API. */
    val endSent: Boolean = false,
) {
    val isOpen: Boolean get() = endedAt == null
}

@Serializable
enum class FuelSyncState {
    @SerialName("synced") SYNCED,
    @SerialName("pendingCreate") PENDING_CREATE,
    @SerialName("pendingUpdate") PENDING_UPDATE,
    @SerialName("pendingDelete") PENDING_DELETE,
}

/** Recarga de gasolina guardada en el teléfono. */
@Serializable
data class FuelEntity(
    val localId: String,
    val serverId: String? = null,
    val tripLocalId: String,
    val number: Int,
    val amount: Double? = null,
    val liters: Double,
    val costCenter: CostCenterEntity? = null,
    /** Foto del recibo en el teléfono (pendiente de subir o copia local). */
    val receiptPath: String? = null,
    val receiptMimeType: String? = null,
    /** URL prefirmada que regresa la API. */
    val receiptUrl: String? = null,
    /** true si la foto cambió y hay que mandarla en el próximo PUT. */
    val receiptChanged: Boolean = false,
    val loadedAt: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val state: FuelSyncState = FuelSyncState.PENDING_CREATE,
    val syncErrorMessage: String? = null,
    /** Código HTTP del último rechazo (4xx = definitivo, no se reintenta solo). */
    val syncErrorCode: Int? = null,
)

/** Todo lo que el registro de viajes tiene pendiente en el teléfono. */
@Serializable
data class RecordingSnapshot(
    val trips: List<TripEntity> = emptyList(),
    val fuel: List<FuelEntity> = emptyList(),
) {
    val activeTrip: TripEntity? get() = trips.firstOrNull { it.running }
}

/** Un punto GPS (una línea del archivo .jsonl del viaje). */
@Serializable
data class TrackPointEntity(
    val lat: Double,
    val lng: Double,
    /** Fecha local "2026-10-19T12:30:15". */
    val t: String,
    val acc: Double? = null,
    val alt: Double? = null,
    val spd: Double? = null,
)
