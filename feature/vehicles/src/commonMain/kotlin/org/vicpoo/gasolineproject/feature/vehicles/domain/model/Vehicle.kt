package org.vicpoo.gasolineproject.feature.vehicles.domain.model

import org.vicpoo.gasolineproject.core.platform.PickedFile

/** Vehículo tal como lo usa la app (independiente de la API y del almacenamiento local). */
data class Vehicle(
    /** Id local estable (existe aunque el vehículo aún no se haya enviado a la API). */
    val id: String,
    /** carroId (Guid) en APIGasoline; null mientras no se sincroniza. */
    val serverId: String?,
    val brand: String,
    val model: String,
    val plates: String,
    val year: Int?,
    val color: String?,
    val mileageKm: Double,
    val tankCapacityLiters: Double?,
    val status: VehicleStatus,
    /** Carro, moto, camioneta o camión (cambia el ícono de la tarjeta). */
    val type: VehicleType = VehicleType.CAR,
    val photo: VehiclePhoto?,
    val syncState: SyncState,
    /** Mensaje de la API si el último intento de sincronizar falló (ej. placas repetidas). */
    val syncErrorMessage: String?,
) {
    /** "Sedán Amarillo" (título de la tarjeta en Figma). */
    val cardTitle: String get() = listOfNotNull(model, color?.takeIf { it.isNotBlank() }).joinToString(" ")

    /** "Volkswagen Sedán". */
    val brandAndModel: String get() = "$brand $model"

    val isPendingSync: Boolean get() = syncState != SyncState.SYNCED
    val hasSyncError: Boolean get() = syncErrorMessage != null
}

/** EstadoCarro de la API: disponible, ocupado, enMantenimiento. */
enum class VehicleStatus {
    AVAILABLE,
    IN_USE,
    MAINTENANCE,
    UNKNOWN;

    companion object {
        fun fromApi(value: String?): VehicleStatus = when (value?.trim()?.lowercase()) {
            null, "", "disponible" -> AVAILABLE
            "ocupado" -> IN_USE
            "enmantenimiento", "en_mantenimiento" -> MAINTENANCE
            else -> UNKNOWN
        }
    }
}

/**
 * TipoVehiculo de la API: "carro", "moto", "camioneta", "camion".
 * [apiValue] es lo que se manda/recibe en el JSON (camelCase, igual que la API).
 */
enum class VehicleType(val apiValue: String) {
    CAR("carro"),
    MOTORCYCLE("moto"),
    PICKUP("camioneta"),
    TRUCK("camion");

    companion object {
        /** Lo desconocido o vacío (carros viejos, API sin la columna) se muestra como carro. */
        fun fromApi(value: String?): VehicleType {
            val v = value?.trim()?.lowercase()
            return entries.firstOrNull { it.apiValue == v } ?: CAR
        }
    }
}

/** Estado de sincronización offline-first. */
enum class SyncState { SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE }

sealed interface VehiclePhoto {
    /** Foto ya subida (URL prefirmada de MinIO que regresa la API). */
    data class Remote(val url: String) : VehiclePhoto

    /** Foto guardada en el teléfono, pendiente de subir. */
    data class Local(val path: String) : VehiclePhoto
}

/** Modelo que entiende Coil para mostrar la foto. */
fun VehiclePhoto.toImageModel(): Any = when (this) {
    is VehiclePhoto.Remote -> url
    is VehiclePhoto.Local -> "file://$path"
}

/** Datos ya validados para crear o editar un vehículo. */
data class VehicleDraft(
    val brand: String,
    val model: String,
    val plates: String,
    val year: Int,
    val color: String,
    val mileageKm: Double,
    val tankCapacityLiters: Double,
    val type: VehicleType = VehicleType.CAR,
    /** Foto nueva elegida por el usuario (null = no cambia). */
    val newPhoto: PickedFile?,
    /** true si el usuario quitó la foto y no eligió otra. */
    val removePhoto: Boolean,
)
