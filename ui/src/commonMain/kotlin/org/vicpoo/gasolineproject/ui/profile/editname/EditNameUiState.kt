package org.vicpoo.gasolineproject.ui.profile.editname

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.ValidationReason

data class EditNameUiState(
    val name: String = "",
    val fieldError: ValidationReason? = null,
    val isSaving: Boolean = false,
    /** Diálogo "¡Error al guardar!" */
    val saveError: AppError? = null,
    /** Diálogo "¡Nombre guardado!" */
    val saved: Boolean = false,
)
