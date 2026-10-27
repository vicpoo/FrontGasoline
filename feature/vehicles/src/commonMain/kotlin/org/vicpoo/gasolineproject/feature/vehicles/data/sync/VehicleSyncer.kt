package org.vicpoo.gasolineproject.feature.vehicles.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.httpCode
import org.vicpoo.gasolineproject.core.common.serverMessage
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.core.sync.PendingSyncTask
import org.vicpoo.gasolineproject.core.sync.SyncRejection
import org.vicpoo.gasolineproject.core.sync.stopsSync
import org.vicpoo.gasolineproject.feature.vehicles.data.local.VehicleEntity
import org.vicpoo.gasolineproject.feature.vehicles.data.local.VehicleLocalDataSource
import org.vicpoo.gasolineproject.feature.vehicles.data.mapper.toRequest
import org.vicpoo.gasolineproject.feature.vehicles.data.remote.CarroDto
import org.vicpoo.gasolineproject.feature.vehicles.data.remote.VehicleApi
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.SyncState

/**
 * Envía a la API los cambios hechos sin conexión (crear / editar / eliminar).
 * - Sin red: se detiene y deja todo pendiente para el siguiente intento.
 * - Error de la API (400, 409 placas repetidas...): guarda el mensaje en ese vehículo
 *   para mostrarlo al usuario y sigue con los demás.
 * - Un rechazo definitivo (4xx) ya no se reenvía solo: espera a que el usuario edite el vehículo
 *   (editar limpia el error). Los errores temporales (5xx) sí se reintentan.
 * - Sin red pide al [BackgroundSyncScheduler] que lo vuelva a intentar en cuanto haya internet.
 */
class VehicleSyncer(
    private val local: VehicleLocalDataSource,
    private val api: VehicleApi,
    private val scheduler: BackgroundSyncScheduler,
) : PendingSyncTask {
    private val mutex = Mutex()

    override suspend fun syncPending(): AppResult<Unit> = mutex.withLock {
        val pending = local.getAll().filter {
            it.syncState != SyncState.SYNCED && !SyncRejection.isPermanent(it.lastSyncErrorCode)
        }
        for (entity in pending) {
            val result = when (entity.syncState) {
                SyncState.PENDING_CREATE -> pushCreate(entity)
                SyncState.PENDING_UPDATE -> pushUpdate(entity)
                SyncState.PENDING_DELETE -> pushDelete(entity)
                SyncState.SYNCED -> AppResult.Success(Unit)
            }
            if (result is AppResult.Failure) {
                if (result.error.stopsSync()) {
                    if (result.error == AppError.NoConnection) scheduler.requestWhenOnline()
                    return@withLock result
                }
                markError(entity, result.error)
            }
        }
        AppResult.Success(Unit)
    }

    private suspend fun pushCreate(entity: VehicleEntity): AppResult<Unit> {
        val dto = when (val created = safeApiCall { api.create(entity.toRequest()) }) {
            is AppResult.Failure -> return created
            is AppResult.Success -> created.data
        }
        val synced = entity.applyServer(dto).copy(syncState = SyncState.SYNCED)
        local.upsert(synced)
        return pushPhoto(synced)
    }

    private suspend fun pushUpdate(entity: VehicleEntity): AppResult<Unit> {
        val serverId = entity.serverId ?: return pushCreate(entity)
        val dto = when (val updated = safeApiCall { api.update(serverId, entity.toRequest()) }) {
            is AppResult.Failure -> return updated
            is AppResult.Success -> updated.data
        }
        val synced = entity.applyServer(dto, keepPhotoUrl = entity.photoPendingDelete).copy(syncState = SyncState.SYNCED)
        local.upsert(synced)
        return pushPhoto(synced)
    }

    private suspend fun pushDelete(entity: VehicleEntity): AppResult<Unit> {
        val serverId = entity.serverId
        if (serverId != null) {
            val deleted = safeApiCall { api.delete(serverId) }
            // 404 = ya no existe en el servidor: igual se borra localmente
            if (deleted is AppResult.Failure && deleted.error != AppError.NotFound) {
                if (deleted.error == AppError.NoConnection || deleted.error == AppError.Unauthorized) return deleted
                // La API no lo dejó borrar (ej. tiene historial): se restaura en la lista con el mensaje
                local.upsert(
                    entity.copy(
                        syncState = SyncState.SYNCED,
                        lastSyncErrorCode = deleted.error.httpCode(),
                        lastSyncErrorMessage = deleted.error.serverMessage(),
                    ),
                )
                return AppResult.Success(Unit)
            }
        }
        entity.localPhotoPath?.let { local.deletePhoto(it) }
        local.remove(entity.localId)
        return AppResult.Success(Unit)
    }

    /** Sube o borra la foto pendiente. Si falla, el vehículo queda "pendiente de editar" para reintentar. */
    private suspend fun pushPhoto(entity: VehicleEntity): AppResult<Unit> {
        val serverId = entity.serverId ?: return AppResult.Success(Unit)

        if (entity.photoPendingDelete) {
            val removed = safeApiCall { api.deletePhoto(serverId) }
            if (removed is AppResult.Failure && removed.error != AppError.NotFound) {
                local.upsert(entity.copy(syncState = SyncState.PENDING_UPDATE))
                return removed
            }
            local.upsert(entity.copy(photoPendingDelete = false, photoUrl = null))
            return AppResult.Success(Unit)
        }

        val path = entity.localPhotoPath
        if (!entity.photoPendingUpload || path == null) return AppResult.Success(Unit)

        val bytes = local.readPhoto(path)
        if (bytes == null) {
            local.upsert(entity.copy(photoPendingUpload = false, localPhotoPath = null))
            return AppResult.Success(Unit)
        }

        val mime = entity.localPhotoMime ?: "image/jpeg"
        val fileName = path.substringAfterLast('/')
        return when (val uploaded = safeApiCall { api.uploadPhoto(serverId, bytes, fileName, mime) }) {
            is AppResult.Failure -> {
                local.upsert(entity.copy(syncState = SyncState.PENDING_UPDATE, photoPendingUpload = true))
                uploaded
            }
            is AppResult.Success -> {
                // La API ya tiene la foto: se usa su URL y se borra la copia local
                local.deletePhoto(path)
                local.upsert(
                    entity.copy(
                        photoUrl = uploaded.data.fotoVehiculoUrl,
                        localPhotoPath = null,
                        localPhotoMime = null,
                        photoPendingUpload = false,
                    ),
                )
                AppResult.Success(Unit)
            }
        }
    }

    private suspend fun markError(entity: VehicleEntity, error: AppError) {
        val current = local.get(entity.localId) ?: return
        local.upsert(current.copy(lastSyncErrorCode = error.httpCode() ?: -1, lastSyncErrorMessage = error.serverMessage()))
    }

    private fun VehicleEntity.applyServer(dto: CarroDto, keepPhotoUrl: Boolean = false) = copy(
        serverId = dto.carroId,
        companyId = dto.empresaId,
        plates = dto.placas,
        status = dto.estado ?: status,
        photoUrl = if (keepPhotoUrl) photoUrl else dto.fotoVehiculoUrl ?: photoUrl,
        lastSyncErrorCode = null,
        lastSyncErrorMessage = null,
    )
}
