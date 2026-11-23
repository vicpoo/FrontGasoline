package org.vicpoo.gasolineproject.data.vehicles.local

import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.domain.vehicles.model.SyncState

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
    /** TipoVehiculo de la API ("carro", "moto"...); null en datos guardados antes de esta versión. */
    val type: String? = null,
    val photoUrl: String? = null,
    val localPhotoPath: String? = null,
    val localPhotoMime: String? = null,
    val photoPendingUpload: Boolean = false,
    /** El usuario quitó la foto sin conexión: falta borrarla en la API. */
    val photoPendingDelete: Boolean = false,
    val syncState: SyncState = SyncState.SYNCED,
    /**
     * Cuándo se hizo el último cambio EN ESTE TELÉFONO (hora local con zona, "2026-11-23T10:00:00-06:00").
     * Se manda como fechaEdicion: si otro celular ya guardó un cambio más reciente, la API no aplica este
     * (409 CAMBIO_MAS_RECIENTE) y se conserva el del servidor. Ver VehicleSyncer.
     */
    val editedAt: String? = null,
    /** Último error de sincronización (código y mensaje de la API). */
    val lastSyncErrorCode: Int? = null,
    val lastSyncErrorMessage: String? = null,
)
