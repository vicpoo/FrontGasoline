package org.vicpoo.gasolineproject.feature.vehicles.data.local

import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.SyncState

/** Vehículo guardado en el dispositivo (incluye datos de sincronización). */
@Serializable
data class VehicleEntity(
    val localId: String,
    /** carroId (Guid) en la API. */
    val serverId: String? = null,
    val companyId: String,
    val brand: String,
    val model: String,
    val plates: String,
    val year: Int? = null,
    val color: String? = null,
    val mileageKm: Double = 0.0,
    val tankCapacityLiters: Double? = null,
    val status: String? = null,
    val photoUrl: String? = null,
    val localPhotoPath: String? = null,
    val localPhotoMime: String? = null,
    val photoPendingUpload: Boolean = false,
    /** El usuario quitó la foto sin conexión: falta borrarla en la API. */
    val photoPendingDelete: Boolean = false,
    val syncState: SyncState = SyncState.SYNCED,
    /** Último error de sincronización (código y mensaje de la API). */
    val lastSyncErrorCode: Int? = null,
    val lastSyncErrorMessage: String? = null,
)
