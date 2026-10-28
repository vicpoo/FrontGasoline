package org.vicpoo.gasolineproject.feature.vehicles.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.data.LastRefreshStore
import org.vicpoo.gasolineproject.core.network.ConnectivityMonitor
import org.vicpoo.gasolineproject.core.network.reconnections
import org.vicpoo.gasolineproject.core.rbac.PermissionChecker
import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.Vehicle
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObservePendingVehicleSyncUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObserveVehiclesUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.RefreshVehiclesUseCase

data class VehicleListUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val query: String = "",
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val pendingSyncCount: Int = 0,
    /** RBAC: mostrar botón "+" */
    val canAdd: Boolean = false,
    val error: AppError? = null,
    /** Cuándo se trajo de la API la copia que se ve (para "Actualizado hace X"). */
    val lastUpdatedAtMillis: Long? = null,
)

/**
 * Para qué se abre la lista:
 * - CATALOG: pestaña Inicio (casa) — ver carros y solicitarlos; sin agregar / editar / eliminar.
 * - FLEET: pestaña Gestión (libreta con lápiz) — administrar la flota; sin "Solicitar".
 */
enum class VehiclesMode { CATALOG, FLEET }

data class VehicleListArgs(val mode: VehiclesMode)

sealed interface VehicleListEvent {
    data class QueryChanged(val value: String) : VehicleListEvent
    data object Refresh : VehicleListEvent
}

class VehicleListViewModel(
    private val args: VehicleListArgs,
    observeVehicles: ObserveVehiclesUseCase,
    observePendingSync: ObservePendingVehicleSyncUseCase,
    private val refreshVehicles: RefreshVehiclesUseCase,
    permissionChecker: PermissionChecker,
    /** Se recarga sola al volver la red y marca "sin conexión" en cuanto se pierde. */
    private val connectivity: ConnectivityMonitor,
    private val lastRefresh: LastRefreshStore,
) : ViewModel() {

    private data class RefreshStatus(
        val isRefreshing: Boolean = false,
        val loadedOnce: Boolean = false,
        val isOffline: Boolean = false,
        val error: AppError? = null,
    )

    private val query = MutableStateFlow("")
    private val refreshStatus = MutableStateFlow(RefreshStatus())

    val state: StateFlow<VehicleListUiState> = combine(
        observeVehicles(),
        observePendingSync(),
        permissionChecker.observe(Privileges.CARRO_CREAR),
        query,
        combine(refreshStatus, connectivity.isOnline, lastRefresh.observe(REFRESH_KEY)) { s, online, last -> Triple(s, online, last) },
    ) { vehicles, pending, canAdd, q, (status, online, lastUpdated) ->
        VehicleListUiState(
            vehicles = vehicles.filter { it.matches(q) },
            query = q,
            isInitialLoading = vehicles.isEmpty() && !status.loadedOnce,
            isRefreshing = status.isRefreshing && status.loadedOnce,
            isOffline = status.isOffline || !online,
            pendingSyncCount = pending,
            canAdd = canAdd && args.mode == VehiclesMode.FLEET,
            error = status.error,
            lastUpdatedAtMillis = lastUpdated,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleListUiState())

    init {
        refresh()
        viewModelScope.launch { connectivity.reconnections().collect { refresh() } }
    }

    fun onEvent(event: VehicleListEvent) {
        when (event) {
            is VehicleListEvent.QueryChanged -> query.value = event.value
            VehicleListEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        if (refreshStatus.value.isRefreshing) return
        viewModelScope.launch {
            refreshStatus.update { it.copy(isRefreshing = true) }
            val result = refreshVehicles()
            val failure = (result as? AppResult.Failure)?.error
            if (failure == null) lastRefresh.markNow(REFRESH_KEY)
            refreshStatus.value = RefreshStatus(
                isRefreshing = false,
                loadedOnce = true,
                isOffline = failure == AppError.NoConnection,
                error = failure?.takeIf { it != AppError.NoConnection },
            )
        }
    }

    private fun Vehicle.matches(q: String): Boolean {
        val text = q.trim()
        if (text.isEmpty()) return true
        return listOfNotNull(brand, model, plates, color).any { it.contains(text, ignoreCase = true) }
    }

    private companion object {
        /** Llave de "Actualizado hace X" de esta lista. */
        const val REFRESH_KEY = "vehicles.list"
    }
}
