package org.vicpoo.gasolineproject.feature.reservations.domain

/** Estado de una solicitud de reserva (enum EstadoSolicitudReserva de APIGasoline). */
enum class ReservationStatus { PENDING, APPROVED, REJECTED, CANCELLED, FINISHED, UNKNOWN }

/**
 * Una solicitud de vehículo (tarjeta de "Solicitudes" y de la campana).
 * La API ya manda el solicitante (nombre y foto) y el carro, así se pinta sin otra llamada.
 */
data class ReservationRequest(
    val id: String,
    val vehicleId: String,
    /** "Vocho" / "Nissan NP300"; null si no se pudo leer el carro. */
    val vehicleTitle: String?,
    val plates: String?,
    val photoUrl: String?,
    val status: ReservationStatus,
    /** Fecha local "2026-10-19T12:30:15". */
    val requestedAt: String?,
    val resolvedAt: String?,
    /** Motivo de quien la aceptó / rechazó (o de la cancelación). */
    val resolutionReason: String?,
    /** Para qué necesita el carro el solicitante (lo escribió al enviarla). */
    val requestReason: String? = null,
    val requesterId: String? = null,
    /** Quién la pidió (tarjeta "Solicitud de ..."). */
    val requesterName: String? = null,
    val requesterPhotoUrl: String? = null,
)

/** Detalle de una solicitud (al tocar la tarjeta en la pestaña llave). */
data class ReservationDetail(
    val request: ReservationRequest,
    val requesterHasLicense: Boolean,
    val requesterLicenseUrl: String?,
    val requesterLicenseUploadedAt: String?,
    val vehicleYear: Int?,
    val vehicleColor: String?,
    val vehicleMileageKm: Double?,
    /** "disponible" | "ocupado" | "enMantenimiento" (como viene de la API). */
    val vehicleState: String?,
    val confirmerName: String?,
    val confirmerPhotoUrl: String?,
)
