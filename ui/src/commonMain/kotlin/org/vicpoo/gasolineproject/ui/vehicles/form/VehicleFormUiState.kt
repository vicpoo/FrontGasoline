package org.vicpoo.gasolineproject.ui.vehicles.form

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.vehicles.model.VehiclePhoto
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleField
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormInput
import org.vicpoo.gasolineproject.domain.vehicles.model.toImageModel

/**
 * Secciones del formulario estilo Airbnb ("Evaluar y continuar"): una tarjeta con cada sección
 * y su botón "Agregar" / "Modificar". Cada sección se edita en una hoja que sube desde abajo
 * con "Cancelar" y "Guardar".
 */
enum class VehicleFormSection(val fields: Set<VehicleField>) {
    BASICS(setOf(VehicleField.BRAND, VehicleField.MODEL)),
    IDENTIFICATION(setOf(VehicleField.PLATES, VehicleField.YEAR, VehicleField.COLOR)),
    SPECS(setOf(VehicleField.MILEAGE, VehicleField.TANK)),
    PHOTO(emptySet()),
}

data class VehicleFormUiState(
    val isEdit: Boolean = false,
    /** Lo ya guardado en la tarjeta. */
    val input: VehicleFormInput = VehicleFormInput(),
    val existingPhoto: VehiclePhoto? = null,
    /** Secciones ya llenas (cuentan en la barra de progreso). */
    val completed: Set<VehicleFormSection> = emptySet(),
    /** Errores al intentar guardar todo (la fila se marca en rojo). */
    val errors: Map<VehicleField, ValidationReason> = emptyMap(),
    // ---- Hoja de edición abierta ----
    val editing: VehicleFormSection? = null,
    /** Copia que se edita en la hoja: "Cancelar" la descarta, "Guardar" la pasa a [input]. */
    val draft: VehicleFormInput = VehicleFormInput(),
    val draftErrors: Map<VehicleField, ValidationReason> = emptyMap(),
    val photoError: ValidationReason? = null,
    // ---- Envío ----
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppError? = null,
) {
    /** Foto guardada en la tarjeta (la nueva o la actual si no se quitó). */
    val photoModel: Any? get() = input.photoModel(existingPhoto)

    /** Foto que se ve dentro de la hoja de edición. */
    val draftPhotoModel: Any? get() = draft.photoModel(existingPhoto)

    val totalSections: Int get() = VehicleFormSection.entries.size

    fun hasError(section: VehicleFormSection): Boolean = section.fields.any { it in errors }
}

private fun VehicleFormInput.photoModel(existing: VehiclePhoto?): Any? =
    newPhoto?.bytes ?: existing?.takeIf { !removePhoto }?.toImageModel()
