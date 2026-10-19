package org.vicpoo.gasolineproject.feature.vehicles.domain.repository

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleDraft

/** Lectura de vehículos (siempre desde lo local: funciona sin red). */
interface VehicleReader {
    fun observeVehicles(): Flow<List<Vehicle>>
    fun observeVehicle(id: String): Flow<Vehicle?>
    fun observePendingSyncCount(): Flow<Int>

    /** Sube cambios pendientes y descarga la lista de la API. */
    suspend fun refresh(): AppResult<Unit>
}

/** Escritura de vehículos (se guarda local primero y se sincroniza después). */
interface VehicleWriter {
    suspend fun create(draft: VehicleDraft): AppResult<Vehicle>
    suspend fun update(id: String, draft: VehicleDraft): AppResult<Vehicle>
    suspend fun delete(id: String): AppResult<Unit>
}

interface VehicleRepository : VehicleReader, VehicleWriter
