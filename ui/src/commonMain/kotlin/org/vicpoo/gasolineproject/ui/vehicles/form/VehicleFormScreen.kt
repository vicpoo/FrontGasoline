package org.vicpoo.gasolineproject.ui.vehicles.form

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.platform.files.rememberDocumentPicker
import org.vicpoo.gasolineproject.ui.common.components.AppScreen
import org.vicpoo.gasolineproject.ui.common.components.ErrorText
import org.vicpoo.gasolineproject.ui.common.components.LoadingView
import org.vicpoo.gasolineproject.ui.common.components.PrimaryButton
import org.vicpoo.gasolineproject.ui.common.components.ReviewCard
import org.vicpoo.gasolineproject.ui.common.components.SecondaryText
import org.vicpoo.gasolineproject.ui.common.components.SegmentedProgress
import org.vicpoo.gasolineproject.ui.common.components.TitleText
import org.vicpoo.gasolineproject.ui.common.components.asText
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormValidator
import org.vicpoo.gasolineproject.ui.vehicles.common.label
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.cd_close
import org.vicpoo.gasolineproject.ui.resources.vehicle_field_photo
import org.vicpoo.gasolineproject.ui.resources.vehicle_form_add_title
import org.vicpoo.gasolineproject.ui.resources.vehicle_form_edit_title
import org.vicpoo.gasolineproject.ui.resources.vehicle_form_offline_note
import org.vicpoo.gasolineproject.ui.resources.vehicle_form_optional
import org.vicpoo.gasolineproject.ui.resources.vehicle_form_photo_added
import org.vicpoo.gasolineproject.ui.resources.vehicle_form_save
import org.vicpoo.gasolineproject.ui.resources.vehicle_form_sections_hint
import org.vicpoo.gasolineproject.ui.resources.vehicle_km_suffix
import org.vicpoo.gasolineproject.ui.resources.vehicle_liters_suffix
import org.vicpoo.gasolineproject.ui.resources.vehicle_section_basics
import org.vicpoo.gasolineproject.ui.resources.vehicle_section_identification
import org.vicpoo.gasolineproject.ui.resources.vehicle_section_specs

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
