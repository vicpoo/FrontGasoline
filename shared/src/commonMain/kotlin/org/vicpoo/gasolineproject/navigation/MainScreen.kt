package org.vicpoo.gasolineproject.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.nav_account
import gasolineproject.shared.generated.resources.nav_history
import gasolineproject.shared.generated.resources.nav_vehicles
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.rbac.PermissionChecker
import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.core.ui.adaptive.AdaptiveNavScaffold
import org.vicpoo.gasolineproject.core.ui.adaptive.NavDestination
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.RefreshPrivilegesUseCase
import org.vicpoo.gasolineproject.feature.profile.presentation.ProfileRoute
import org.vicpoo.gasolineproject.feature.trips.presentation.history.TripHistoryRoute
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehicleListRoute

private const val TAB_VEHICLES = "vehicles"
private const val TAB_HISTORY = "history"
private const val TAB_PROFILE = "profile"

/**
 * Al entrar a la pantalla principal pregunta a la API si cambiaron los privilegios
 * (GET /api/auth/privilegios con If-None-Match). Sin red se usan los guardados.
 */
class MainViewModel(
    permissionChecker: PermissionChecker,
    private val refreshPrivileges: RefreshPrivilegesUseCase,
) : ViewModel() {

    val privileges: StateFlow<Set<String>> = permissionChecker.observePrivileges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch { refreshPrivileges() }
    }
}

/**
 * Pantalla principal con navegación adaptativa.
 * RBAC: cada pestaña aparece solo si el usuario tiene su privilegio.
 * Aquí se irán agregando Reservas, Viajes, Cargas y Reportes.
 */
@Composable
fun MainScreen(
    onVehicleClick: (String) -> Unit,
    onAddVehicle: () -> Unit,
    onTripClick: (String) -> Unit,
    onEditName: () -> Unit,
    onUpdateLicense: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: MainViewModel = koinViewModel(),
) {
    val privileges by viewModel.privileges.collectAsStateWithLifecycle()

    val destinations = buildList {
        if (Privileges.CARRO_VER in privileges) {
            add(NavDestination(TAB_VEHICLES, stringResource(Res.string.nav_vehicles), AppIcons.Car))
        }
        if (Privileges.VIAJE_VER_PROPIOS in privileges || Privileges.VIAJE_VER_TODOS in privileges) {
            add(NavDestination(TAB_HISTORY, stringResource(Res.string.nav_history), AppIcons.History))
        }
        add(NavDestination(TAB_PROFILE, stringResource(Res.string.nav_account), AppIcons.User))
    }

    var selectedKey by rememberSaveable { mutableStateOf(TAB_VEHICLES) }
    val currentKey = destinations.firstOrNull { it.key == selectedKey }?.key ?: destinations.first().key

    AdaptiveNavScaffold(
        destinations = destinations,
        selectedKey = currentKey,
        onSelect = { selectedKey = it },
    ) {
        when (currentKey) {
            TAB_VEHICLES -> VehicleListRoute(onVehicleClick = onVehicleClick, onAddVehicle = onAddVehicle)
            TAB_HISTORY -> TripHistoryRoute(onTripClick = onTripClick)
            else -> ProfileRoute(
                onEditName = onEditName,
                onUpdateLicense = onUpdateLicense,
                onLoggedOut = onLoggedOut,
            )
        }
    }
}
