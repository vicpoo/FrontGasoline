package org.vicpoo.gasolineproject.ui.profile

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.profile.Profile

data class ProfileUiState(
    val profile: Profile? = null,
    val showPhotoOptions: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val photoError: AppError? = null,
    /** Foto pendiente para el botón "Reintentar". */
    val pendingPhoto: PickedFile? = null,
    /** "Salir": se está intentando mandar lo pendiente antes de cerrar sesión. */
    val isCheckingLogout: Boolean = false,
    /** Registros que solo existen en el teléfono y se perderían al salir (muestra el aviso). */
    val logoutPendingCount: Int? = null,
)
