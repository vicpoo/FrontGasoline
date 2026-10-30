package org.vicpoo.gasolineproject.feature.vehicles.data.mapper

import org.vicpoo.gasolineproject.feature.vehicles.data.local.VehicleEntity
import org.vicpoo.gasolineproject.feature.vehicles.data.remote.CarroDto
import org.vicpoo.gasolineproject.feature.vehicles.data.remote.CarroRequestDto
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.SyncState
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehiclePhoto
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleStatus
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleType

fun VehicleEntity.toDomain() = Vehicle(
    id = localId,
    serverId = serverId,
    brand = brand,
    model = model,
    plates = plates,
    year = year,
    color = color,
    mileageKm = mileageKm,
    tankCapacityLiters = tankCapacityLiters,
    status = VehicleStatus.fromApi(status),
    type = VehicleType.fromApi(type),
    photo = when {
        localPhotoPath != null -> VehiclePhoto.Local(localPhotoPath)
        photoUrl != null -> VehiclePhoto.Remote(photoUrl)
        else -> null
    },
    syncState = syncState,
    syncErrorMessage = if (lastSyncErrorCode != null) lastSyncErrorMessage.orEmpty() else null,
)

fun CarroDto.toEntity() = VehicleEntity(
    localId = "srv-$carroId",
    serverId = carroId,
    companyId = empresaId,
    brand = marca,
    model = modelo,
    plates = placas,
    year = anio,
    color = color,
    mileageKm = kilometrajeActual,
    tankCapacityLiters = capacidadTanque,
    status = estado,
    type = tipoVehiculo,
    photoUrl = fotoVehiculoUrl?.takeIf { it.isNotBlank() },
    syncState = SyncState.SYNCED,
)

fun VehicleEntity.toRequest() = CarroRequestDto(
    empresaId = companyId,
    marca = brand,
    modelo = model,
    anio = year,
    color = color,
    placas = plates,
    capacidadTanque = tankCapacityLiters,
    kilometrajeActual = mileageKm,
    tipoVehiculo = VehicleType.fromApi(type).apiValue,
)
