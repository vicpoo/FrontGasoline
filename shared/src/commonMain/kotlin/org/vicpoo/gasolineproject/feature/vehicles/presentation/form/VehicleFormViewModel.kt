package org.vicpoo.gasolineproject.feature.vehicles.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehiclePhoto
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.toImageModel
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObserveVehicleUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.SaveVehicleUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.validation.VehicleField
import org.vicpoo.gasolineproject.feature.vehicles.domain.validation.VehicleFormInput
import org.vicpoo.gasolineproject.feature.vehicles.domain.validation.VehicleFormValidator
import org.vicpoo.gasolineproject.core.common.formatDecimal

/** vehicleId == null -> Agregar · vehicleId != null -> Editar */
data class VehicleFormArgs(val vehicleId: String?)

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

sealed interface VehicleFormEvent {
    /** "Agregar" / "Modificar" de una fila: abre la hoja de esa sección. */
    data class OpenSection(val section: VehicleFormSection) : VehicleFormEvent
    data object CancelSection : VehicleFormEvent
    data object ConfirmSection : VehicleFormEvent

    data class FieldChanged(val field: VehicleField, val value: String) : VehicleFormEvent
    data class PhotoPicked(val file: PickedFile?) : VehicleFormEvent
    data object RemovePhoto : VehicleFormEvent

    data object Save : VehicleFormEvent
}

sealed interface VehicleFormEffect {
    data object Saved : VehicleFormEffect
    data object Close : VehicleFormEffect
}

class VehicleFormViewModel(
    private val args: VehicleFormArgs,
    private val observeVehicle: ObserveVehicleUseCase,
    private val saveVehicle: SaveVehicleUseCase,
    private val validator: VehicleFormValidator,
) : ViewModel() {

    private val _state = MutableStateFlow(VehicleFormUiState(isEdit = args.vehicleId != null))
    val state: StateFlow<VehicleFormUiState> = _state.asStateFlow()

    private val _effects = Channel<VehicleFormEffect>(Channel.BUFFERED)
    val effects: Flow<VehicleFormEffect> = _effects.receiveAsFlow()

    init {
        args.vehicleId?.let(::loadVehicle)
    }

    fun onEvent(event: VehicleFormEvent) {
        when (event) {
            is VehicleFormEvent.OpenSection -> _state.update {
                it.copy(
                    editing = event.section,
                    draft = it.input,
                    draftErrors = it.errors.filterKeys { field -> field in event.section.fields },
                    photoError = null,
                )
            }
            VehicleFormEvent.CancelSection -> _state.update { it.copy(editing = null, draftErrors = emptyMap(), photoError = null) }
            VehicleFormEvent.ConfirmSection -> confirmSection()
            is VehicleFormEvent.FieldChanged -> _state.update {
                it.copy(draft = it.draft.with(event.field, event.value), draftErrors = it.draftErrors - event.field)
            }
            is VehicleFormEvent.PhotoPicked -> onPhotoPicked(event.file)
            VehicleFormEvent.RemovePhoto -> _state.update {
                it.copy(draft = it.draft.copy(newPhoto = null, removePhoto = true), photoError = null)
            }
            VehicleFormEvent.Save -> save()
        }
    }

    private fun loadVehicle(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val vehicle = observeVehicle(id).first()
            if (vehicle == null) {
                _state.update { it.copy(isLoading = false, error = AppError.NotFound) }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    input = vehicle.toFormInput(),
                    existingPhoto = vehicle.photo,
                    // Al editar todo ya está lleno: solo se modifica la sección que se quiera
                    completed = VehicleFormSection.entries.toSet(),
                )
            }
        }
    }

    private fun onPhotoPicked(file: PickedFile?) {
        if (file == null) return // canceló
        val error = validator.validatePhoto(file)
        _state.update {
            if (error != null) {
                it.copy(photoError = error)
            } else {
                it.copy(draft = it.draft.copy(newPhoto = file, removePhoto = false), photoError = null)
            }
        }
    }

    /** "Guardar" de la hoja: valida solo esa sección y la pasa a la tarjeta. */
    private fun confirmSection() {
        val current = _state.value
        val section = current.editing ?: return
        if (current.photoError != null) return

        val sectionErrors = validator.validate(current.draft, section.fields)
        if (sectionErrors.isNotEmpty()) {
            _state.update { it.copy(draftErrors = sectionErrors) }
            return
        }
        _state.update {
            it.copy(
                input = it.draft,
                completed = it.completed + section,
                errors = it.errors - section.fields,
                editing = null,
                draftErrors = emptyMap(),
            )
        }
    }

    /** "Guardar vehículo": valida todo; si falta algo, abre la hoja de la primera sección con error. */
    private fun save() {
        val current = _state.value
        if (current.isSaving) return

        val allErrors = validator.validate(current.input)
        if (allErrors.isNotEmpty()) {
            val firstWithError = VehicleFormSection.entries.first { section -> section.fields.any { it in allErrors } }
            _state.update {
                it.copy(
                    errors = allErrors,
                    editing = firstWithError,
                    draft = it.input,
                    draftErrors = allErrors.filterKeys { field -> field in firstWithError.fields },
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            when (val result = saveVehicle(args.vehicleId, current.input)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _effects.send(VehicleFormEffect.Saved)
                }
                is AppResult.Failure -> _state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }
}

private fun Vehicle.toFormInput() = VehicleFormInput(
    brand = brand,
    model = model,
    plates = plates,
    year = year?.toString().orEmpty(),
    color = color.orEmpty(),
    mileage = formatDecimal(mileageKm),
    tank = tankCapacityLiters?.let(::formatDecimal).orEmpty(),
)
