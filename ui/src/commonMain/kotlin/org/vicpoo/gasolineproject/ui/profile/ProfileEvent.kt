package org.vicpoo.gasolineproject.ui.profile

import org.vicpoo.gasolineproject.domain.files.PickedFile

sealed interface ProfileEvent {
    data object EditPhotoClicked : ProfileEvent
    data object DismissPhotoOptions : ProfileEvent
    data class PhotoPicked(val file: PickedFile?) : ProfileEvent
    data object RetryPhoto : ProfileEvent
    data object DismissPhotoError : ProfileEvent
    data object Logout : ProfileEvent

    /** En el aviso de pendientes: "Salir de todos modos" (se pierden) o "Cancelar". */
    data object LogoutAnyway : ProfileEvent
    data object DismissLogoutWarning : ProfileEvent

    /** Botón sol / luna: cambia a oscuro si ahora se ve claro, y al revés. */
    data class ToggleTheme(val currentlyDark: Boolean, val systemDark: Boolean) : ProfileEvent
}
