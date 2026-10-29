package org.vicpoo.gasolineproject.feature.triprecording.presentation.start

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.location.rememberLocationPermission
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.AppScreen
import org.vicpoo.gasolineproject.core.ui.components.AppTextButton
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.PrimaryButton
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.SubtitleText
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenter
import org.vicpoo.gasolineproject.feature.triprecording.presentation.common.AssignedVehicleCard
import org.vicpoo.gasolineproject.feature.triprecording.resources.Res
import org.vicpoo.gasolineproject.feature.triprecording.resources.start_trip_action
import org.vicpoo.gasolineproject.feature.triprecording.resources.start_trip_cancel
import org.vicpoo.gasolineproject.feature.triprecording.resources.start_trip_cost_center
import org.vicpoo.gasolineproject.feature.triprecording.resources.start_trip_no_cost_centers
import org.vicpoo.gasolineproject.feature.triprecording.resources.start_trip_permission_denied
import org.vicpoo.gasolineproject.feature.triprecording.resources.start_trip_select_cost_center
import org.vicpoo.gasolineproject.feature.triprecording.resources.start_trip_title
import org.vicpoo.gasolineproject.feature.triprecording.resources.start_trip_vehicle_not_found

@Composable
fun StartTripRoute(
    reservationId: String,
    onBack: () -> Unit,
    onTripStarted: () -> Unit,
    viewModel: StartTripViewModel = koinViewModel(key = "start-trip-$reservationId") {
        parametersOf(StartTripArgs(reservationId))
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberLocationPermission { granted ->
        viewModel.onEvent(StartTripEvent.PermissionResult(granted))
    }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                StartTripEffect.RequestLocationPermission -> permission.launch()
                StartTripEffect.TripStarted -> onTripStarted()
            }
        }
    }
    StartTripScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
fun StartTripScreen(
    state: StartTripUiState,
    onEvent: (StartTripEvent) -> Unit,
    onBack: () -> Unit,
) {
    val dimens = AppTheme.dimens
    AppScreen {
        Spacer(Modifier.height(dimens.spaceM))
        AppBackButton(onClick = onBack)
        Spacer(Modifier.height(dimens.spaceM))
        TitleText(stringResource(Res.string.start_trip_title))
        Spacer(Modifier.height(dimens.spaceL))

        when {
            state.isLoading -> LoadingView()
            state.vehicle == null -> ErrorText(stringResource(Res.string.start_trip_vehicle_not_found))
            else -> {
                AssignedVehicleCard(vehicle = state.vehicle)
                Spacer(Modifier.height(dimens.spaceL))

                SubtitleText(stringResource(Res.string.start_trip_cost_center))
                Spacer(Modifier.height(dimens.spaceS))
                if (state.costCenters.isEmpty()) {
                    SecondaryText(stringResource(Res.string.start_trip_no_cost_centers))
                }
                state.costCenters.forEach { costCenter ->
                    CostCenterOption(
                        costCenter = costCenter,
                        selected = costCenter.id == state.selectedCostCenterId,
                        onSelect = { onEvent(StartTripEvent.CostCenterSelected(costCenter.id)) },
                    )
                    Spacer(Modifier.height(dimens.spaceS))
                }
                if (state.showCostCenterError) ErrorText(stringResource(Res.string.start_trip_select_cost_center))
                if (state.permissionDenied) ErrorText(stringResource(Res.string.start_trip_permission_denied))
                state.error?.let { error ->
                    ErrorText(if (error == AppError.NotFound) stringResource(Res.string.start_trip_vehicle_not_found) else error.asText())
                }

                Spacer(Modifier.height(dimens.spaceL))
                PrimaryButton(
                    text = stringResource(Res.string.start_trip_action),
                    onClick = { onEvent(StartTripEvent.StartClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = state.isStarting,
                    leadingIcon = AppIcons.Play,
                )
                Spacer(Modifier.height(dimens.spaceS))
                AppTextButton(
                    text = stringResource(Res.string.start_trip_cancel),
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = !state.isStarting,
                )
                Spacer(Modifier.height(dimens.spaceL))
            }
        }
    }
}

/** Opción de la lista "Centro de costos" (radio + nombre + "CDMX • CC-104"). */
@Composable
private fun CostCenterOption(costCenter: CostCenter, selected: Boolean, onSelect: () -> Unit) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        shape = RoundedCornerShape(dimens.radius),
        color = colors.background,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.divider),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimens.spaceS, vertical = dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.text),
            )
            Spacer(Modifier.width(dimens.spaceS))
            Column(Modifier.weight(1f)) {
                Text(costCenter.name, style = AppTheme.typography.body, color = colors.title)
                Text(costCenter.key, style = AppTheme.typography.caption, color = colors.textSecondary)
            }
        }
    }
}
