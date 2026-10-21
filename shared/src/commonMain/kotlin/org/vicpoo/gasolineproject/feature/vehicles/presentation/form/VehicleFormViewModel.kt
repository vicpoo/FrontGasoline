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

/** Pasos del formulario estilo Airbnb: una idea por pantalla. */
enum class VehicleFormStep(val fields: Set<VehicleField>) {
    BASICS(setOf(VehicleField.BRAND, VehicleField.MODEL)),
    IDENTIFICATION(setOf(VehicleField.PLATES, VehicleField.YEAR, VehicleField.COLOR)),
    SPECS(setOf(VehicleField.MILEAGE, VehicleField.TANK)),
    PHOTO(emptySet()),
    REVIEW(emptySet()),
}

data class VehicleFormUiState(
    val isEdit: Boolean = false,
    val step: VehicleFormStep = VehicleFormStep.BASICS,
    val input: VehicleFormInput = VehicleFormInput(),
    val existingPhoto: VehiclePhoto? = null,
    val errors: Map<VehicleField, ValidationReason> = emptyMap(),
    val photoError: ValidationReason? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppError? = null,
    /** true si el usuario vino desde "Revisar" a corregir un paso: "Siguiente" lo regresa a Revisar. */
    val returnToReview: Boolean = false,
) {
    val stepIndex: Int get() = step.ordinal
    val totalSteps: Int get() = VehicleFormStep.entries.size
    val isLastStep: Boolean get() = step == VehicleFormStep.REVIEW

    /** Lo que se muestra en el selector de foto: la nueva, o la actual si no se quitó. */
    val photoModel: Any?
        get() = input.newPhoto?.bytes ?: existingPhoto?.takeIf { !input.removePhoto }?.toImageModel()
}

sealed interface VehicleFormEvent {
    data class FieldChanged(val field: VehicleField, val value: String) : VehicleFormEvent
    data class PhotoPicked(val file: PickedFile?) : VehicleFormEvent
    data object RemovePhoto : VehicleFormEvent
    data object Next : VehicleFormEvent
    data object Back : VehicleFormEvent
    data class GoToStep(val step: VehicleFormStep) : VehicleFormEvent
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
            is VehicleFormEvent.FieldChanged -> _state.update {
                it.copy(input = it.input.with(event.field, event.value), errors = it.errors - event.field, error = null)
            }
            is VehicleFormEvent.PhotoPicked -> onPhotoPicked(event.file)
            VehicleFormEvent.RemovePhoto -> _state.update {
                it.copy(input = it.input.copy(newPhoto = null, removePhoto = true), photoError = null)
            }
            VehicleFormEvent.Next -> onNext()
            VehicleFormEvent.Back -> onBack()
            is VehicleFormEvent.GoToStep -> _state.update {
                it.copy(step = event.step, returnToReview = it.step == VehicleFormStep.REVIEW)
            }
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
                    // Al editar se empieza en el resumen: el usuario elige qué sección cambiar
                    step = VehicleFormStep.REVIEW,
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
                it.copy(input = it.input.copy(newPhoto = file, removePhoto = false), photoError = null)
            }
        }
    }

    private fun onNext() {
        val current = _state.value
        if (current.isSaving) return

        if (current.isLastStep) {
            save()
            return
        }

        val stepErrors = validator.validate(current.input, current.step.fields)
        if (stepErrors.isNotEmpty()) {
            _state.update { it.copy(errors = it.errors + stepErrors) }
            return
        }

        val nextStep = if (current.returnToReview) {
            VehicleFormStep.REVIEW
        } else {
            VehicleFormStep.entries[current.stepIndex + 1]
        }
        _state.update { it.copy(step = nextStep, returnToReview = false) }
    }

    private fun onBack() {
        val current = _state.value
        when {
            current.returnToReview -> _state.update { it.copy(step = VehicleFormStep.REVIEW, returnToReview = false) }
            // Primer paso, o el resumen al editar: "Atrás" cierra el formulario
            current.stepIndex == 0 || (current.isEdit && current.isLastStep) ->
                viewModelScope.launch { _effects.send(VehicleFormEffect.Close) }
            else -> _state.update { it.copy(step = VehicleFormStep.entries[current.stepIndex - 1]) }
        }
    }

    private fun save() {
        val current = _state.value
        val allErrors = validator.validate(current.input)
        if (allErrors.isNotEmpty()) {
            // Lleva al primer paso que tenga un error
            val firstStepWithError = VehicleFormStep.entries.first { step -> step.fields.any { it in allErrors } }
            _state.update { it.copy(errors = allErrors, step = firstStepWithError, returnToReview = true) }
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
