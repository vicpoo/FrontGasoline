package org.vicpoo.gasolineproject.feature.triprecording.data

import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.feature.triprecording.data.local.AssignedVehicleEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.CostCenterEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.FuelEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.FuelSyncState
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.AssignedCarroDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.CentroCostoDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.SolicitudReservaDto
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicle
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenter
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecord
import org.vicpoo.gasolineproject.feature.triprecording.domain.ReceiptPhoto
import org.vicpoo.gasolineproject.feature.triprecording.domain.RecordSyncState
import org.vicpoo.gasolineproject.feature.triprecording.domain.RecordedTrip
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripPhase

// ---------- API -> teléfono ----------

fun SolicitudReservaDto.toAssignedEntity(car: AssignedCarroDto) = AssignedVehicleEntity(
    reservationId = solicitudReservaId,
    vehicleId = carroId,
    brand = car.marca,
    model = car.modelo,
    plates = car.placas,
    color = car.color,
    photoUrl = car.fotoVehiculoUrl,
    mileage = car.kilometrajeActual,
    tankCapacityLiters = car.capacidadTanque,
    vehicleType = car.tipoVehiculo,
)

fun CentroCostoDto.toEntity() = CostCenterEntity(id = centroCostoId, name = nombre, key = clave)

// ---------- teléfono <-> dominio ----------

fun AssignedVehicleEntity.toDomain() = AssignedVehicle(
    reservationId = reservationId,
    vehicleId = vehicleId,
    brand = brand,
    model = model,
    plates = plates,
    color = color,
    photoUrl = photoUrl,
    mileage = mileage,
    tankCapacityLiters = tankCapacityLiters,
    vehicleType = vehicleType,
)

fun AssignedVehicle.toEntity() = AssignedVehicleEntity(
    reservationId = reservationId,
    vehicleId = vehicleId,
    brand = brand,
    model = model,
    plates = plates,
    color = color,
    photoUrl = photoUrl,
    mileage = mileage,
    tankCapacityLiters = tankCapacityLiters,
    vehicleType = vehicleType,
)

fun CostCenterEntity.toDomain() = CostCenter(id = id, name = name, key = key)

fun CostCenter.toEntity() = CostCenterEntity(id = id, name = name, key = key)

fun TripEntity.toDomain(): RecordedTrip = RecordedTrip(
    localId = localId,
    serverId = serverId,
    vehicle = vehicle.toDomain(),
    costCenter = costCenter?.toDomain(),
    startedAt = startedAt,
    startedAtMillis = startedAtMillis,
    endedAt = endedAt,
    endedAtMillis = endedAtMillis,
    distanceMeters = distanceMeters,
    lastLocation = if (lastLat != null && lastLng != null) GeoPoint(lastLat, lastLng) else null,
    isRunning = running,
    syncState = when {
        syncErrorMessage != null -> RecordSyncState.ERROR
        serverId == null || (!running && !closedOnServer) -> RecordSyncState.PENDING
        else -> RecordSyncState.SYNCED
    },
    syncErrorMessage = syncErrorMessage,
    phase = phase(),
    initialMileage = initialMileage,
    finalMileage = finalMileage,
    closedPausesMillis = pauses.sumOf { p -> p.endedAtMillis?.let { it - p.startedAtMillis } ?: 0L },
    openPauseStartedAtMillis = pauses.lastOrNull { it.isOpen }?.startedAtMillis,
)

/** Fase del viaje según lo guardado en el teléfono. */
fun TripEntity.phase(): TripPhase = when {
    !running -> TripPhase.CLOSED
    endedAt != null -> TripPhase.STOPPED
    pauses.any { it.isOpen } -> TripPhase.PAUSED
    else -> TripPhase.RUNNING
}

fun FuelEntity.toDomain(): FuelRecord = FuelRecord(
    localId = localId,
    serverId = serverId,
    tripLocalId = tripLocalId,
    number = number,
    amount = amount,
    liters = liters,
    costCenter = costCenter?.toDomain(),
    // Se prefiere la copia local: se ve aunque no haya red
    receipt = when {
        receiptPath != null -> ReceiptPhoto.Local(receiptPath)
        receiptUrl != null -> ReceiptPhoto.Remote(receiptUrl)
        else -> null
    },
    loadedAt = loadedAt,
    location = if (lat != null && lng != null) GeoPoint(lat, lng) else null,
    syncState = when {
        syncErrorMessage != null -> RecordSyncState.ERROR
        state == FuelSyncState.SYNCED -> RecordSyncState.SYNCED
        else -> RecordSyncState.PENDING
    },
    syncErrorMessage = syncErrorMessage,
)

