package org.vicpoo.gasolineproject.data.vehicles.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppLog
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.httpCode
import org.vicpoo.gasolineproject.domain.common.serverMessage
import org.vicpoo.gasolineproject.data.network.safeApiCall
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.domain.sync.PendingSyncKind
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.domain.sync.SyncRejection
import org.vicpoo.gasolineproject.domain.sync.stopsSync
import org.vicpoo.gasolineproject.data.vehicles.local.VehicleEntity
import org.vicpoo.gasolineproject.data.vehicles.local.VehicleLocalDataSource
import org.vicpoo.gasolineproject.data.vehicles.mapper.toRequest
import org.vicpoo.gasolineproject.data.vehicles.remote.CarroDto
import org.vicpoo.gasolineproject.data.vehicles.remote.VehicleApi
import org.vicpoo.gasolineproject.domain.vehicles.model.SyncState

/**
 * Envía a la API los cambios hechos sin conexión (crear / editar / eliminar).
 * - Sin red: se detiene y deja todo pendiente para el siguiente intento.
 * - Error de la API (400, 409 placas repetidas...): guarda el mensaje en ese vehículo
 *   para mostrarlo al usuario y sigue con los demás.
 * - Un rechazo definitivo (4xx) ya no se reenvía solo: espera a que el usuario edite el vehículo
 *   (editar limpia el error). Los errores temporales (5xx) sí se reintentan.
 * - Sin red pide al [BackgroundSyncScheduler] que lo vuelva a intentar en cuanto haya internet.
 * - Conflicto entre celulares: gana el cambio MÁS RECIENTE (no el último que sincroniza). Cada edición
 *   lleva su hora (fechaEdicion); si otro celular ya guardó una más nueva, la API responde
 *   409 CAMBIO_MAS_RECIENTE y aquí se descarta el cambio local y se conserva el del servidor.
 */
class VehicleSyncer(
    private val local: VehicleLocalDataSource,
    private val api: VehicleApi,
    private val scheduler: BackgroundSyncScheduler,
) : PendingSyncTask {
    private val mutex = Mutex()

    override val kind = PendingSyncKind.VEHICLES

    /** Vehículos creados, editados o borrados sin conexión que todavía no llegan a la API. */
    override suspend fun pendingCount(): Int = local.getAll().count { it.syncState != SyncState.SYNCED }

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
            is AppResult.Failure -> return if (updated.error.isNewerChangeOnServer()) keepServerVersion(entity) else updated
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

    /**
     * Otro celular cambió este vehículo DESPUÉS que este: se descarta el cambio local (también la foto
     * pendiente) y la siguiente actualización trae la versión del servidor. Se deja el aviso en el vehículo.
     */
    private suspend fun keepServerVersion(entity: VehicleEntity): AppResult<Unit> {
        entity.localPhotoPath?.let { local.deletePhoto(it) }
        local.upsert(
            entity.copy(
                syncState = SyncState.SYNCED,
                localPhotoPath = null,
                localPhotoMime = null,
                photoPendingUpload = false,
                photoPendingDelete = false,
                lastSyncErrorCode = null,
                lastSyncErrorMessage = NEWER_CHANGE_MESSAGE,
            ),
        )
        AppLog.i("Vehiculos", "Cambio descartado: otro celular guardó uno más reciente")
        return AppResult.Success(Unit)
    }

    private fun AppError.isNewerChangeOnServer(): Boolean =
        this is AppError.Server && code == 409 && problemCode == NEWER_CHANGE_CODE

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

/** Código del ProblemDetails de la API: ya hay un cambio más reciente de ese carro (otro celular). */
internal const val NEWER_CHANGE_CODE = "CAMBIO_MAS_RECIENTE"

/** Lo que ve el usuario en el vehículo hasta la siguiente actualización. */
internal const val NEWER_CHANGE_MESSAGE = "Otro celular guardó un cambio más reciente de este vehículo; se conservó ese."
