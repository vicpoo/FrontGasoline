package org.vicpoo.gasolineproject.ui.profile

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.profile.Profile
import org.vicpoo.gasolineproject.domain.session.OpenTripSummary

data class ProfileUiState(
    val profile: Profile? = null,
    val showPhotoOptions: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val photoError: AppError? = null,
    /** Foto pendiente para el botón "Reintentar". */
    val pendingPhoto: PickedFile? = null,
    /** "Salir": se está intentando mandar lo pendiente antes de cerrar sesión. */
    val isCheckingLogout: Boolean = false,
    /** Aviso antes de "Salir" (viaje abierto que hay que cerrar y/o registros que se borrarían). */
    val logoutWarning: LogoutWarning? = null,
)

/**
 * Lo que se le dice al usuario antes de cerrar sesión.
 * @param openTrip viaje abierto: antes de salir se cierra con kilometraje final y foto (queda interrumpido).
 * @param lostCount registros sin enviar (solicitudes, vehículos...) que SÍ se borran del teléfono.
 */
data class LogoutWarning(
    val openTrip: OpenTripSummary?,
    val lostCount: Int,
)
