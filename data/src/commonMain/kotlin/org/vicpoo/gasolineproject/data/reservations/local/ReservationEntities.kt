package org.vicpoo.gasolineproject.data.reservations.local

import kotlinx.serialization.Serializable

/** Solicitud guardada en el teléfono (con los datos del carro y del solicitante para verla sin red). */
@Serializable
data class ReservationRequestEntity(
    val id: String,
    val vehicleId: String,
    val vehicleTitle: String? = null,
    val plates: String? = null,
    val photoUrl: String? = null,
    /** Tal como viene de la API: "pendiente", "aprobada", "rechazada", "cancelada", "finalizada". */
    val status: String,
    val requestedAt: String? = null,
    val resolvedAt: String? = null,
    val resolutionReason: String? = null,
    /** Motivo que escribió el solicitante (v8 de la API). */
    val requestReason: String? = null,
    val requesterId: String? = null,
    val requesterName: String? = null,
    val requesterPhotoUrl: String? = null,
)

/**
 * Número del badge de la campana (respuestas a mis solicitudes sin leer). Las solicitudes en sí van
 * una por fila en ListItemEntity ("my_reservations" y "company_reservations"); este documento conserva
 * el nombre del archivo de antes, así su copia vieja se pasa y se borra sola.
 */
@Serializable
data class MyReservationsSnapshot(
    val unseenUpdates: Int = 0,
)
