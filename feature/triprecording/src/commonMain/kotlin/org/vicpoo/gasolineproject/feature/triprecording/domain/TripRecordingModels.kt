package org.vicpoo.gasolineproject.feature.triprecording.domain

import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.platform.PickedFile

/**
 * Carro que el usuario puede usar para un viaje: viene de una solicitud de reserva APROBADA
 * (GET /api/solicitudes-reserva?empleadoSolicitanteId=...&estado=aprobada) + los datos del carro.
 */
data class AssignedVehicle(
    val reservationId: String,
    val vehicleId: String,
    val brand: String,
    val model: String,
    val plates: String,
    val color: String?,
    val photoUrl: String?,
    /** Kilometraje actual del carro: será el kilometraje inicial del viaje. */
    val mileage: Double,
    val tankCapacityLiters: Double?,
) {
    val title: String get() = "$brand $model".trim()
}

/** Centro de costos de la empresa (GET /api/centros-costo?empresaId=...). */
data class CostCenter(
    val id: String,
    val name: String,
    val key: String,
)

/** Estado de envío al servidor de cualquier registro hecho en el teléfono. */
enum class RecordSyncState { SYNCED, PENDING, ERROR }

/**
 * En qué punto va el viaje:
 * RUNNING (GPS encendido) → PAUSED (GPS apagado) → STOPPED (botón ■: falta kilometraje y foto) → CLOSED.
 */
enum class TripPhase { RUNNING, PAUSED, STOPPED, CLOSED }

/** Viaje que se está registrando (o que terminó y falta enviar). */
data class RecordedTrip(
    val localId: String,
    val serverId: String?,
    val vehicle: AssignedVehicle,
    val costCenter: CostCenter?,
    /** Fecha local "2026-10-19T12:30:15". */
    val startedAt: String,
    val startedAtMillis: Long,
    val endedAt: String?,
    val endedAtMillis: Long?,
    val distanceMeters: Double,
    val lastLocation: GeoPoint?,
    val isRunning: Boolean,
    val syncState: RecordSyncState,
    val syncErrorMessage: String?,
    val phase: TripPhase = if (isRunning) TripPhase.RUNNING else TripPhase.CLOSED,
    /** Kilometraje del carro al iniciar (el final no puede ser menor). */
    val initialMileage: Double = vehicle.mileage,
    /** Kilometraje final capturado (si ya se guardó "Registrar Viaje"). */
    val finalMileage: Double? = null,
    /** Tiempo en pausas ya cerradas. */
    val closedPausesMillis: Long = 0L,
    /** Inicio de la pausa abierta (null = no está en pausa). */
    val openPauseStartedAtMillis: Long? = null,
) {
    val distanceKm: Double get() = distanceMeters / 1000.0

    val isPaused: Boolean get() = phase == TripPhase.PAUSED

    /** Duración real del viaje: desde el inicio hasta [nowMillis] (o la hora de fin) sin contar las pausas. */
    fun activeMillis(nowMillis: Long): Long {
        val end = endedAtMillis ?: nowMillis
        val openPause = openPauseStartedAtMillis?.let { (end - it).coerceAtLeast(0L) } ?: 0L
        return (end - startedAtMillis - closedPausesMillis - openPause).coerceAtLeast(0L)
    }
}

/** Foto del recibo: guardada en el teléfono (pendiente) o ya en el servidor. */
sealed interface ReceiptPhoto {
    data class Local(val path: String) : ReceiptPhoto
    data class Remote(val url: String) : ReceiptPhoto
}

/** Recarga de gasolina registrada durante el viaje. */
data class FuelRecord(
    val localId: String,
    val serverId: String?,
    val tripLocalId: String,
    /** Número consecutivo dentro del viaje ("Recarga 1#"). */
    val number: Int,
    val amount: Double?,
    val liters: Double,
    val costCenter: CostCenter?,
    val receipt: ReceiptPhoto?,
    val loadedAt: String,
    val location: GeoPoint?,
    val syncState: RecordSyncState,
    val syncErrorMessage: String?,
)

/** Datos que captura el formulario "Agregar / Editar registro de gasolina". */
data class FuelRecordDraft(
    /** null = nuevo registro. */
    val localId: String?,
    val amountText: String,
    val litersText: String,
    val costCenterId: String?,
    /** Foto nueva elegida (bytes); null = conservar la actual. */
    val newReceipt: PickedFile?,
    /** true si ya tiene una foto guardada (al editar). */
    val hasCurrentReceipt: Boolean,
)

/** Qué pasó después de guardar algo en el teléfono e intentar enviarlo. */
sealed interface SendOutcome {
    /** Llegó al servidor. */
    data object Sent : SendOutcome

    /** Sin conexión: quedó guardado en el teléfono y se enviará solo después. */
    data object SavedOffline : SendOutcome

    /** El servidor lo rechazó (ej. litros mayores al tanque). Queda guardado con el mensaje. */
    data class Rejected(val message: String?) : SendOutcome
}

/** Estado en vivo del registro GPS (lo muestra la pantalla del viaje en curso). */
data class TrackingSession(
    val trip: RecordedTrip? = null,
    /** Ruta capturada hasta ahora (para dibujarla en el mapa). */
    val route: List<GeoPoint> = emptyList(),
    val current: GeoPoint? = null,
    /** true si llegó una lectura del GPS en los últimos segundos. */
    val gpsActive: Boolean = false,
    val hasLocationPermission: Boolean = false,
)
