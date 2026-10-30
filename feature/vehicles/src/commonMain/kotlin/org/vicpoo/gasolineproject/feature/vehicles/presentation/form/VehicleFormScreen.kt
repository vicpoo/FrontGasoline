package org.vicpoo.gasolineproject.feature.vehicles.presentation.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.core.platform.rememberDocumentPicker
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.AppTextField
import org.vicpoo.gasolineproject.core.ui.components.EditSheet
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.PhotoPickerBox
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.ReviewCard
import org.vicpoo.gasolineproject.core.ui.components.ReviewRow
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.SegmentedProgress
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleType
import org.vicpoo.gasolineproject.feature.vehicles.domain.validation.VehicleField
import org.vicpoo.gasolineproject.feature.vehicles.domain.validation.VehicleFormValidator
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.icon
import org.vicpoo.gasolineproject.feature.vehicles.presentation.common.label
import org.vicpoo.gasolineproject.feature.vehicles.resources.Res
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_add_short
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_cancel
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_modify
import org.vicpoo.gasolineproject.feature.vehicles.resources.action_save
import org.vicpoo.gasolineproject.feature.vehicles.resources.cd_close
import org.vicpoo.gasolineproject.feature.vehicles.resources.cd_remove_photo
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_brand
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_color
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_mileage_initial
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_model
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_photo
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_plates
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_tank
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_type
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_field_year
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_add_title
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_edit_title
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_new_vehicle
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_offline_note
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_optional
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_photo_added
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_photo_change
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_photo_empty
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_save
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_section_error
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_section_pending
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_form_sections_hint
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_hint_brand
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_hint_color
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_hint_mileage
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_hint_model
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_hint_plates
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_hint_tank
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_hint_year
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_km_suffix
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_liters_suffix
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_section_basics
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_section_identification
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_section_specs
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_step_basics_subtitle
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_step_basics_title
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_step_identification_subtitle
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_step_identification_title
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_step_photo_subtitle
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_step_photo_title
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_step_specs_subtitle
import org.vicpoo.gasolineproject.feature.vehicles.resources.vehicle_step_specs_title

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
 * Agregar / Editar vehículo estilo Airbnb ("Evaluar y continuar"):
 * una tarjeta con el carro arriba y una fila por sección con su botón "Agregar" / "Modificar".
 * Cada sección se edita en una hoja que sube desde abajo (Cancelar / Guardar).
 * Abajo, la barra de avance en segmentos y "Guardar vehículo".
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
    val dimens = AppTheme.dimens
    val input = state.input
    val km = stringResource(Res.string.vehicle_km_suffix)
    val liters = stringResource(Res.string.vehicle_liters_suffix)

    AppScreen {
        // Título arriba, a la altura de la X para cerrar (misma posición que los títulos de las pestañas)
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            TitleText(
                stringResource(if (state.isEdit) Res.string.vehicle_form_edit_title else Res.string.vehicle_form_add_title),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(AppIcons.Close, contentDescription = stringResource(Res.string.cd_close), tint = AppTheme.colors.title)
            }
        }
        Spacer(Modifier.height(dimens.spaceXs))
        SecondaryText(stringResource(Res.string.vehicle_form_sections_hint))
        Spacer(Modifier.height(dimens.spaceL))

        ReviewCard {
            VehicleHeader(state)
            HorizontalDivider(color = AppTheme.colors.divider)

            val brandModel = listOf(input.brand, input.model).filter { it.isNotBlank() }.joinToString(" ")
            SectionRow(
                state, onEvent, VehicleFormSection.BASICS, Res.string.vehicle_section_basics,
                // "Moto · Italika FT150" (el tipo solo se muestra cuando ya hay marca/modelo)
                value = brandModel.takeIf { it.isNotBlank() }?.let { "${input.type.label()} · $it" }.orEmpty(),
            )
            SectionRow(
                state, onEvent, VehicleFormSection.IDENTIFICATION, Res.string.vehicle_section_identification,
                value = listOf(input.plates, input.year, input.color).filter { it.isNotBlank() }.joinToString(" · "),
            )
            SectionRow(
                state, onEvent, VehicleFormSection.SPECS, Res.string.vehicle_section_specs,
                value = listOfNotNull(
                    input.mileage.takeIf { it.isNotBlank() }?.let { "$it $km" },
                    input.tank.takeIf { it.isNotBlank() }?.let { "$it $liters" },
                ).joinToString(" · "),
            )
            SectionRow(
                state, onEvent, VehicleFormSection.PHOTO, Res.string.vehicle_field_photo,
                value = stringResource(if (state.photoModel != null) Res.string.vehicle_form_photo_added else Res.string.vehicle_form_optional),
                showDivider = false,
            )
        }

        Spacer(Modifier.height(dimens.spaceM))
        SecondaryText(stringResource(Res.string.vehicle_form_offline_note))
        state.error?.let {
            Spacer(Modifier.height(dimens.spaceS))
            ErrorText(it.asText())
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(dimens.spaceL))
        SegmentedProgress(total = state.totalSections, done = state.completed.size)
        Spacer(Modifier.height(dimens.spaceM))
        PrimaryButton(
            text = stringResource(Res.string.vehicle_form_save),
            onClick = { onEvent(VehicleFormEvent.Save) },
            isLoading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    state.editing?.let { section ->
        SectionSheet(section = section, state = state, onEvent = onEvent, onPickPhoto = onPickPhoto, km = km, liters = liters)
    }
}

/** Arriba de la tarjeta: foto (si hay) y "Marca Modelo" (como el alojamiento en Airbnb). */
@Composable
private fun VehicleHeader(state: VehicleFormUiState) {
    val dimens = AppTheme.dimens
    val title = listOf(state.input.brand, state.input.model).filter { it.isNotBlank() }.joinToString(" ")
        .ifBlank { stringResource(Res.string.vehicle_form_new_vehicle) }
    Row(Modifier.fillMaxWidth().padding(vertical = dimens.spaceM), verticalAlignment = Alignment.CenterVertically) {
        AppImage(
            model = state.photoModel,
            contentDescription = null,
            fallbackIcon = state.input.type.icon,
            modifier = Modifier.size(width = 96.dp, height = 80.dp).clip(RoundedCornerShape(dimens.radius)),
        )
        Spacer(Modifier.width(dimens.spaceM))
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTheme.typography.subtitle.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.title)
            if (state.input.plates.isNotBlank()) {
                SecondaryText(state.input.plates)
            }
        }
    }
}

@Composable
private fun SectionRow(
    state: VehicleFormUiState,
    onEvent: (VehicleFormEvent) -> Unit,
    section: VehicleFormSection,
    title: StringResource,
    value: String,
    showDivider: Boolean = true,
) {
    val hasError = state.hasError(section)
    val filled = section in state.completed
    ReviewRow(
        title = stringResource(title),
        value = when {
            hasError -> stringResource(Res.string.vehicle_form_section_error)
            value.isBlank() -> stringResource(Res.string.vehicle_form_section_pending)
            else -> value
        },
        actionText = stringResource(if (filled) Res.string.action_modify else Res.string.action_add_short),
        // Sin llenar: solo "+" (más limpio); ya llena: "Modificar"
        actionIcon = if (filled) null else AppIcons.Plus,
        onAction = { onEvent(VehicleFormEvent.OpenSection(section)) },
        isError = hasError,
        showDivider = showDivider,
    )
}

/** Hoja de edición de una sección (Airbnb "Modifica el número de huéspedes"). */
@Composable
private fun SectionSheet(
    section: VehicleFormSection,
    state: VehicleFormUiState,
    onEvent: (VehicleFormEvent) -> Unit,
    onPickPhoto: () -> Unit,
    km: String,
    liters: String,
) {
    val (title, subtitle) = when (section) {
        VehicleFormSection.BASICS -> Res.string.vehicle_step_basics_title to Res.string.vehicle_step_basics_subtitle
        VehicleFormSection.IDENTIFICATION -> Res.string.vehicle_step_identification_title to Res.string.vehicle_step_identification_subtitle
        VehicleFormSection.SPECS -> Res.string.vehicle_step_specs_title to Res.string.vehicle_step_specs_subtitle
        VehicleFormSection.PHOTO -> Res.string.vehicle_step_photo_title to Res.string.vehicle_step_photo_subtitle
    }
    EditSheet(
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        cancelText = stringResource(Res.string.action_cancel),
        saveText = stringResource(Res.string.action_save),
        onCancel = { onEvent(VehicleFormEvent.CancelSection) },
        onSave = { onEvent(VehicleFormEvent.ConfirmSection) },
        closeDescription = stringResource(Res.string.cd_close),
    ) {
        when (section) {
            VehicleFormSection.BASICS -> {
                VehicleTypePicker(
                    selected = state.draft.type,
                    onSelect = { onEvent(VehicleFormEvent.TypeChanged(it)) },
                )
                Spacer(Modifier.height(AppTheme.dimens.spaceL))
                FormField(state, onEvent, VehicleField.BRAND, Res.string.vehicle_field_brand, Res.string.vehicle_hint_brand, capitalization = KeyboardCapitalization.Words)
                FormField(
                    state, onEvent, VehicleField.MODEL, Res.string.vehicle_field_model, Res.string.vehicle_hint_model,
                    capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done,
                )
            }
            VehicleFormSection.IDENTIFICATION -> {
                FormField(state, onEvent, VehicleField.PLATES, Res.string.vehicle_field_plates, Res.string.vehicle_hint_plates, capitalization = KeyboardCapitalization.Characters)
                FormField(state, onEvent, VehicleField.YEAR, Res.string.vehicle_field_year, Res.string.vehicle_hint_year, keyboardType = KeyboardType.Number)
                FormField(
                    state, onEvent, VehicleField.COLOR, Res.string.vehicle_field_color, Res.string.vehicle_hint_color,
                    capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done,
                )
            }
            VehicleFormSection.SPECS -> {
                FormField(
                    state, onEvent, VehicleField.MILEAGE, Res.string.vehicle_field_mileage_initial, Res.string.vehicle_hint_mileage,
                    keyboardType = KeyboardType.Number, suffix = km,
                )
                FormField(
                    state, onEvent, VehicleField.TANK, Res.string.vehicle_field_tank, Res.string.vehicle_hint_tank,
                    keyboardType = KeyboardType.Decimal, suffix = liters, imeAction = ImeAction.Done,
                )
            }
            VehicleFormSection.PHOTO -> {
                PhotoPickerBox(
                    photoModel = state.draftPhotoModel,
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
        }
    }
}

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
) {
    AppTextField(
        value = state.draft.valueOf(field),
        onValueChange = { onEvent(VehicleFormEvent.FieldChanged(field, it)) },
        label = stringResource(label),
        placeholder = stringResource(hint),
        modifier = Modifier.fillMaxWidth(),
        errorMessage = state.draftErrors[field]?.asText(),
        suffix = suffix,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = imeAction,
        ),
        // "Listo" en el teclado = "Guardar" de la hoja
        keyboardActions = KeyboardActions(onDone = { onEvent(VehicleFormEvent.ConfirmSection) }),
    )
    Spacer(Modifier.height(AppTheme.dimens.spaceS))
}

/**
 * Tipo de vehículo como en Airbnb ("¿Qué tipo de alojamiento?"): tarjetas de 2 en 2 con el ícono
 * y el nombre; la elegida lleva borde grueso del color principal.
 */
@Composable
private fun VehicleTypePicker(selected: VehicleType, onSelect: (VehicleType) -> Unit) {
    val dimens = AppTheme.dimens
    Text(
        text = stringResource(Res.string.vehicle_field_type),
        style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
        color = AppTheme.colors.title,
    )
    Spacer(Modifier.height(dimens.spaceS))
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
        VehicleType.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                row.forEach { type ->
                    VehicleTypeOption(
                        type = type,
                        isSelected = type == selected,
                        onClick = { onSelect(type) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Si quedara una sola en la fila, que no se estire al doble
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VehicleTypeOption(type: VehicleType, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { selected = isSelected },
        shape = RoundedCornerShape(dimens.radius),
        color = if (isSelected) colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.06f) else colors.background,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) colors.primary else colors.textSecondary.copy(alpha = if (colors.isDark) 0.45f else 0.25f),
        ),
    ) {
        Column(Modifier.padding(dimens.spaceM)) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = if (isSelected) colors.primary else colors.title,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(dimens.spaceS))
            Text(
                text = type.label(),
                style = AppTheme.typography.body.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                color = colors.title,
            )
        }
    }
}

