package org.vicpoo.gasolineproject.feature.vehicles.presentation.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.action_back
import gasolineproject.shared.generated.resources.action_change
import gasolineproject.shared.generated.resources.action_next
import gasolineproject.shared.generated.resources.action_save
import gasolineproject.shared.generated.resources.cd_remove_photo
import gasolineproject.shared.generated.resources.vehicle_field_brand
import gasolineproject.shared.generated.resources.vehicle_field_color
import gasolineproject.shared.generated.resources.vehicle_field_mileage_initial
import gasolineproject.shared.generated.resources.vehicle_field_model
import gasolineproject.shared.generated.resources.vehicle_field_photo
import gasolineproject.shared.generated.resources.vehicle_field_plates
import gasolineproject.shared.generated.resources.vehicle_field_tank
import gasolineproject.shared.generated.resources.vehicle_field_year
import gasolineproject.shared.generated.resources.vehicle_form_add_title
import gasolineproject.shared.generated.resources.vehicle_form_edit_title
import gasolineproject.shared.generated.resources.vehicle_form_no_photo
import gasolineproject.shared.generated.resources.vehicle_form_offline_note
import gasolineproject.shared.generated.resources.vehicle_form_photo_change
import gasolineproject.shared.generated.resources.vehicle_form_photo_empty
import gasolineproject.shared.generated.resources.vehicle_form_step_label
import gasolineproject.shared.generated.resources.vehicle_hint_brand
import gasolineproject.shared.generated.resources.vehicle_hint_color
import gasolineproject.shared.generated.resources.vehicle_hint_mileage
import gasolineproject.shared.generated.resources.vehicle_hint_model
import gasolineproject.shared.generated.resources.vehicle_hint_plates
import gasolineproject.shared.generated.resources.vehicle_hint_tank
import gasolineproject.shared.generated.resources.vehicle_hint_year
import gasolineproject.shared.generated.resources.vehicle_km_suffix
import gasolineproject.shared.generated.resources.vehicle_liters_suffix
import gasolineproject.shared.generated.resources.vehicle_section_basics
import gasolineproject.shared.generated.resources.vehicle_section_identification
import gasolineproject.shared.generated.resources.vehicle_section_specs
import gasolineproject.shared.generated.resources.vehicle_step_basics_subtitle
import gasolineproject.shared.generated.resources.vehicle_step_basics_title
import gasolineproject.shared.generated.resources.vehicle_step_identification_subtitle
import gasolineproject.shared.generated.resources.vehicle_step_identification_title
import gasolineproject.shared.generated.resources.vehicle_step_photo_subtitle
import gasolineproject.shared.generated.resources.vehicle_step_photo_title
import gasolineproject.shared.generated.resources.vehicle_step_review_subtitle
import gasolineproject.shared.generated.resources.vehicle_step_review_title
import gasolineproject.shared.generated.resources.vehicle_step_specs_subtitle
import gasolineproject.shared.generated.resources.vehicle_step_specs_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.core.platform.rememberDocumentPicker
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.components.AppTextButton
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.InfoRow
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.PhotoPickerBox
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.WizardScaffold
import org.vicpoo.gasolineproject.core.ui.components.WizardStepHeader
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.vehicles.domain.validation.VehicleField
import org.vicpoo.gasolineproject.feature.vehicles.domain.validation.VehicleFormValidator

@Composable
fun VehicleFormRoute(
    vehicleId: String?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    viewModel: VehicleFormViewModel = koinViewModel(key = "vehicle-form-${vehicleId ?: "new"}") {
        parametersOf(VehicleFormArgs(vehicleId))
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                VehicleFormEffect.Saved -> onSaved()
                VehicleFormEffect.Close -> onClose()
            }
        }
    }

    val photoPicker = rememberDocumentPicker(
        mimeTypes = VehicleFormValidator.PHOTO_MIME_TYPES,
        onResult = { file -> viewModel.onEvent(VehicleFormEvent.PhotoPicked(file)) },
    )

    VehicleFormScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onClose = onClose,
        onPickPhoto = photoPicker::launch,
    )
}

/**
 * Agregar / Editar vehículo estilo Airbnb:
 * en vez de un formulario largo, 5 pasos cortos con una pregunta cada uno y un resumen final.
 */
@Composable
fun VehicleFormScreen(
    state: VehicleFormUiState,
    onEvent: (VehicleFormEvent) -> Unit,
    onClose: () -> Unit,
    onPickPhoto: () -> Unit,
) {
    if (state.isLoading) {
        LoadingView()
        return
    }

    WizardScaffold(
        flowTitle = stringResource(if (state.isEdit) Res.string.vehicle_form_edit_title else Res.string.vehicle_form_add_title),
        stepLabel = stringResource(Res.string.vehicle_form_step_label, state.stepIndex + 1, state.totalSteps),
        currentStep = state.stepIndex,
        totalSteps = state.totalSteps,
        backText = stringResource(Res.string.action_back),
        nextText = stringResource(if (state.isLastStep) Res.string.action_save else Res.string.action_next),
        onClose = onClose,
        onBack = { onEvent(VehicleFormEvent.Back) },
        onNext = { onEvent(VehicleFormEvent.Next) },
        isNextLoading = state.isSaving,
        isBackEnabled = state.stepIndex > 0 || state.returnToReview || state.isEdit,
    ) { stepIndex ->
        when (VehicleFormStep.entries[stepIndex]) {
            VehicleFormStep.BASICS -> BasicsStep(state, onEvent)
            VehicleFormStep.IDENTIFICATION -> IdentificationStep(state, onEvent)
            VehicleFormStep.SPECS -> SpecsStep(state, onEvent)
            VehicleFormStep.PHOTO -> PhotoStep(state, onEvent, onPickPhoto)
            VehicleFormStep.REVIEW -> ReviewStep(state, onEvent)
        }
    }
}

// ---------------- Paso 1: Marca y modelo ----------------

@Composable
private fun ColumnScope.BasicsStep(state: VehicleFormUiState, onEvent: (VehicleFormEvent) -> Unit) {
    WizardStepHeader(
        title = stringResource(Res.string.vehicle_step_basics_title),
        subtitle = stringResource(Res.string.vehicle_step_basics_subtitle),
    )
    FormField(state, onEvent, VehicleField.BRAND, Res.string.vehicle_field_brand, Res.string.vehicle_hint_brand, capitalization = KeyboardCapitalization.Words)
    FieldSpacer()
    FormField(
        state, onEvent, VehicleField.MODEL, Res.string.vehicle_field_model, Res.string.vehicle_hint_model,
        capitalization = KeyboardCapitalization.Words,
        imeAction = ImeAction.Done,
    )
}

// ---------------- Paso 2: Identificación ----------------

@Composable
private fun ColumnScope.IdentificationStep(state: VehicleFormUiState, onEvent: (VehicleFormEvent) -> Unit) {
    WizardStepHeader(
        title = stringResource(Res.string.vehicle_step_identification_title),
        subtitle = stringResource(Res.string.vehicle_step_identification_subtitle),
    )
    FormField(state, onEvent, VehicleField.PLATES, Res.string.vehicle_field_plates, Res.string.vehicle_hint_plates, capitalization = KeyboardCapitalization.Characters)
    FieldSpacer()
    FormField(state, onEvent, VehicleField.YEAR, Res.string.vehicle_field_year, Res.string.vehicle_hint_year, keyboardType = KeyboardType.Number)
    FieldSpacer()
    FormField(
        state, onEvent, VehicleField.COLOR, Res.string.vehicle_field_color, Res.string.vehicle_hint_color,
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Done,
    )
}

// ---------------- Paso 3: Datos técnicos ----------------

@Composable
private fun ColumnScope.SpecsStep(state: VehicleFormUiState, onEvent: (VehicleFormEvent) -> Unit) {
    WizardStepHeader(
        title = stringResource(Res.string.vehicle_step_specs_title),
        subtitle = stringResource(Res.string.vehicle_step_specs_subtitle),
    )
    FormField(
        state, onEvent, VehicleField.MILEAGE, Res.string.vehicle_field_mileage_initial, Res.string.vehicle_hint_mileage,
        keyboardType = KeyboardType.Number,
        suffix = stringResource(Res.string.vehicle_km_suffix),
    )
    FieldSpacer()
    FormField(
        state, onEvent, VehicleField.TANK, Res.string.vehicle_field_tank, Res.string.vehicle_hint_tank,
        keyboardType = KeyboardType.Decimal,
        suffix = stringResource(Res.string.vehicle_liters_suffix),
        imeAction = ImeAction.Done,
    )
}

// ---------------- Paso 4: Foto ----------------

@Composable
private fun ColumnScope.PhotoStep(
    state: VehicleFormUiState,
    onEvent: (VehicleFormEvent) -> Unit,
    onPickPhoto: () -> Unit,
) {
    WizardStepHeader(
        title = stringResource(Res.string.vehicle_step_photo_title),
        subtitle = stringResource(Res.string.vehicle_step_photo_subtitle),
    )
    PhotoPickerBox(
        photoModel = state.photoModel,
        emptyText = stringResource(Res.string.vehicle_form_photo_empty),
        changeText = stringResource(Res.string.vehicle_form_photo_change),
        removeDescription = stringResource(Res.string.cd_remove_photo),
        onPick = onPickPhoto,
        onRemove = { onEvent(VehicleFormEvent.RemovePhoto) },
    )
    state.photoError?.let {
        Spacer(Modifier.height(AppTheme.dimens.spaceS))
        ErrorText(it.asText())
    }
}

// ---------------- Paso 5: Revisar ----------------

@Composable
private fun ColumnScope.ReviewStep(state: VehicleFormUiState, onEvent: (VehicleFormEvent) -> Unit) {
    val dimens = AppTheme.dimens
    val input = state.input
    WizardStepHeader(
        title = stringResource(Res.string.vehicle_step_review_title),
        subtitle = stringResource(Res.string.vehicle_step_review_subtitle),
    )

    ReviewSection(Res.string.vehicle_section_basics, onEdit = { onEvent(VehicleFormEvent.GoToStep(VehicleFormStep.BASICS)) }) {
        InfoRow(stringResource(Res.string.vehicle_field_brand), input.brand)
        InfoRow(stringResource(Res.string.vehicle_field_model), input.model, showDivider = false)
    }
    Spacer(Modifier.height(dimens.spaceM))
    ReviewSection(Res.string.vehicle_section_identification, onEdit = { onEvent(VehicleFormEvent.GoToStep(VehicleFormStep.IDENTIFICATION)) }) {
        InfoRow(stringResource(Res.string.vehicle_field_plates), input.plates)
        InfoRow(stringResource(Res.string.vehicle_field_year), input.year)
        InfoRow(stringResource(Res.string.vehicle_field_color), input.color, showDivider = false)
    }
    Spacer(Modifier.height(dimens.spaceM))
    ReviewSection(Res.string.vehicle_section_specs, onEdit = { onEvent(VehicleFormEvent.GoToStep(VehicleFormStep.SPECS)) }) {
        InfoRow(stringResource(Res.string.vehicle_field_mileage_initial), "${input.mileage} ${stringResource(Res.string.vehicle_km_suffix)}")
        InfoRow(stringResource(Res.string.vehicle_field_tank), "${input.tank} ${stringResource(Res.string.vehicle_liters_suffix)}", showDivider = false)
    }
    Spacer(Modifier.height(dimens.spaceM))
    ReviewSection(Res.string.vehicle_field_photo, onEdit = { onEvent(VehicleFormEvent.GoToStep(VehicleFormStep.PHOTO)) }) {
        val photo = state.photoModel
        if (photo != null) {
            AppImage(
                model = photo,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(dimens.radiusS)),
            )
        } else {
            SecondaryText(stringResource(Res.string.vehicle_form_no_photo))
        }
    }

    Spacer(Modifier.height(dimens.spaceL))
    SecondaryText(stringResource(Res.string.vehicle_form_offline_note))
    state.error?.let {
        Spacer(Modifier.height(dimens.spaceS))
        ErrorText(it.asText())
    }
}

/** Tarjeta del resumen con título y enlace "Cambiar" (como en Airbnb). */
@Composable
private fun ReviewSection(
    title: StringResource,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(title),
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = AppTheme.colors.title,
                )
                AppTextButton(text = stringResource(Res.string.action_change), onClick = onEdit)
            }
            content()
        }
    }
}

// ---------------- Helpers ----------------

@Composable
private fun FormField(
    state: VehicleFormUiState,
    onEvent: (VehicleFormEvent) -> Unit,
    field: VehicleField,
    label: StringResource,
    hint: StringResource,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    suffix: String? = null,
    helper: String? = null,
) {
    AppTextField(
        value = state.input.valueOf(field),
        onValueChange = { onEvent(VehicleFormEvent.FieldChanged(field, it)) },
        label = stringResource(label),
        placeholder = stringResource(hint),
        modifier = Modifier.fillMaxWidth(),
        errorMessage = state.errors[field]?.asText(),
        helperText = helper,
        suffix = suffix,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onEvent(VehicleFormEvent.Next) }),
    )
}

@Composable
private fun FieldSpacer() {
    Spacer(Modifier.height(AppTheme.dimens.spaceL))
}
