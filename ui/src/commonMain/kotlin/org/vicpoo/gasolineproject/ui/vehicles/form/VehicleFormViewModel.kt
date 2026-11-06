package org.vicpoo.gasolineproject.ui.vehicles.form

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
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.formatDecimal
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.vehicles.model.Vehicle
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObserveVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.SaveVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormInput
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormValidator

/** vehicleId == null -> Agregar · vehicleId != null -> Editar */
data class VehicleFormArgs(val vehicleId: String?)

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
            is VehicleFormEvent.TypeChanged -> _state.update { it.copy(draft = it.draft.copy(type = event.type)) }
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
    type = type,
)
