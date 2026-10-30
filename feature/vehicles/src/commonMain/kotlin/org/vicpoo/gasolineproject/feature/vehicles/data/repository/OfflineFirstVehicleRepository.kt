package org.vicpoo.gasolineproject.feature.vehicles.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.map
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.feature.vehicles.data.local.VehicleEntity
import org.vicpoo.gasolineproject.feature.vehicles.data.local.VehicleLocalDataSource
import org.vicpoo.gasolineproject.feature.vehicles.data.mapper.toDomain
import org.vicpoo.gasolineproject.feature.vehicles.data.mapper.toEntity
import org.vicpoo.gasolineproject.feature.vehicles.data.remote.VehicleApi
import org.vicpoo.gasolineproject.feature.vehicles.data.sync.VehicleSyncer
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.SyncState
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleDraft
import org.vicpoo.gasolineproject.feature.vehicles.domain.repository.VehicleRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Offline-first:
 * 1. La UI SIEMPRE lee de lo local (funciona sin red).
 * 2. Crear / editar se guarda local al instante con estado PENDING_* y se sincroniza en segundo plano.
 * 3. Eliminar intenta la API primero (para mostrar si no se puede borrar, ej. tiene historial);
 *    si no hay red queda pendiente y se envía después.
 */
class OfflineFirstVehicleRepository(
    private val local: VehicleLocalDataSource,
    private val api: VehicleApi,
    private val syncer: VehicleSyncer,
    private val sessionReader: SessionReader,
    private val appScope: AppScope,
) : VehicleRepository {

    override fun observeVehicles(): Flow<List<Vehicle>> = local.vehicles.map { list ->
        list.filter { it.syncState != SyncState.PENDING_DELETE }
            .map { it.toDomain() }
            .sortedBy { it.cardTitle.lowercase() }
    }

    override fun observeVehicle(id: String): Flow<Vehicle?> = local.vehicles.map { list ->
        list.firstOrNull { it.localId == id && it.syncState != SyncState.PENDING_DELETE }?.toDomain()
    }

    override fun observePendingSyncCount(): Flow<Int> =
        local.vehicles.map { list -> list.count { it.syncState != SyncState.SYNCED } }

    override suspend fun refresh(): AppResult<Unit> {
        val pushed = syncer.syncPending()
        if (pushed is AppResult.Failure && (pushed.error == AppError.NoConnection || pushed.error == AppError.Unauthorized)) {
            return pushed
        }
        val companyId = sessionReader.current()?.companyId
        return safeApiCall { api.getAll(companyId) }.map { remote ->
            // Los carros dados de baja ya no se muestran en la app
            local.replaceSynced(remote.filterNot { it.dadoDeBaja }.map { it.toEntity() })
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(draft: VehicleDraft): AppResult<Vehicle> {
        // El carro pertenece a la empresa del empleado (el Administrador interno no tiene empresa)
        val companyId = sessionReader.current()?.companyId ?: return AppResult.Failure(AppError.NoCompany)

        val localId = "loc-${Uuid.random()}"
        val photo = draft.newPhoto?.let { savePhoto(localId, it) }
        val entity = VehicleEntity(
            localId = localId,
            companyId = companyId,
            brand = draft.brand,
            model = draft.model,
            plates = draft.plates,
            year = draft.year,
            color = draft.color,
            mileageKm = draft.mileageKm,
            tankCapacityLiters = draft.tankCapacityLiters,
            type = draft.type.apiValue,
            status = "disponible",
            localPhotoPath = photo?.path,
            localPhotoMime = photo?.mimeType,
            photoPendingUpload = photo != null,
            syncState = SyncState.PENDING_CREATE,
        )
        local.upsert(entity)
        scheduleSync()
        return AppResult.Success(entity.toDomain())
    }

    override suspend fun update(id: String, draft: VehicleDraft): AppResult<Vehicle> {
        val existing = local.get(id) ?: return AppResult.Failure(AppError.NotFound)

        var updated = existing.copy(
            brand = draft.brand,
            model = draft.model,
            plates = draft.plates,
            year = draft.year,
            color = draft.color,
            mileageKm = draft.mileageKm,
            tankCapacityLiters = draft.tankCapacityLiters,
            type = draft.type.apiValue,
            syncState = if (existing.syncState == SyncState.PENDING_CREATE) SyncState.PENDING_CREATE else SyncState.PENDING_UPDATE,
            lastSyncErrorCode = null,
            lastSyncErrorMessage = null,
        )

        val newPhoto = draft.newPhoto
        if (newPhoto != null) {
            existing.localPhotoPath?.let { local.deletePhoto(it) }
            val saved = savePhoto(id, newPhoto)
            updated = updated.copy(
                localPhotoPath = saved.path,
                localPhotoMime = saved.mimeType,
                photoPendingUpload = true,
                photoPendingDelete = false,
            )
        } else if (draft.removePhoto) {
            existing.localPhotoPath?.let { local.deletePhoto(it) }
            updated = updated.copy(
                localPhotoPath = null,
                localPhotoMime = null,
                photoPendingUpload = false,
                // Si ya tenía foto en la API, hay que borrarla allá también
                photoPendingDelete = existing.serverId != null && existing.photoUrl != null,
                photoUrl = null,
            )
        }

        local.upsert(updated)
        scheduleSync()
        return AppResult.Success(updated.toDomain())
    }

    override suspend fun delete(id: String): AppResult<Unit> {
        val existing = local.get(id) ?: return AppResult.Failure(AppError.NotFound)
        val serverId = existing.serverId

        if (serverId == null) {
            // Nunca llegó a la API: basta con borrarlo localmente
            existing.localPhotoPath?.let { local.deletePhoto(it) }
            local.remove(id)
            return AppResult.Success(Unit)
        }

        return when (val result = safeApiCall { api.delete(serverId) }) {
            is AppResult.Success -> {
                removeLocally(existing)
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> when (result.error) {
                AppError.NotFound -> {
                    removeLocally(existing)
                    AppResult.Success(Unit)
                }
                AppError.NoConnection -> {
                    // Sin red: se oculta y se borrará en la API cuando vuelva la conexión
                    local.upsert(existing.copy(syncState = SyncState.PENDING_DELETE))
                    AppResult.Success(Unit)
                }
                // Ej. 409: el carro tiene historial y la API no deja borrarlo (debe darse de baja)
                else -> result
            }
        }
    }

    private suspend fun removeLocally(entity: VehicleEntity) {
        entity.localPhotoPath?.let { local.deletePhoto(it) }
        local.remove(entity.localId)
    }

    private fun scheduleSync() {
        appScope.scope.launch { syncer.syncPending() }
    }

    private class SavedPhoto(val path: String, val mimeType: String)

    private suspend fun savePhoto(localId: String, file: PickedFile): SavedPhoto {
        val extension = when {
            file.mimeType.contains("png") -> "png"
            file.mimeType.contains("webp") -> "webp"
            else -> "jpg"
        }
        val path = local.savePhoto(localId, file.bytes, extension)
        return SavedPhoto(path, file.mimeType)
    }
}
