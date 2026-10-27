package org.vicpoo.gasolineproject.feature.reservations.data.local

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

/** "Solicitudes de la empresa" (pestaña llave), guardadas para verlas sin red. */
@Serializable
data class CompanyReservationsSnapshot(
    val requests: List<ReservationRequestEntity> = emptyList(),
)

/** Mis solicitudes (campana) y el último número del badge. */
@Serializable
data class MyReservationsSnapshot(
    val requests: List<ReservationRequestEntity> = emptyList(),
    val unseenUpdates: Int = 0,
)
