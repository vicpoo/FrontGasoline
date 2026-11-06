package org.vicpoo.gasolineproject.ui.vehicles.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.LastRefreshStore
import org.vicpoo.gasolineproject.domain.connectivity.ConnectivityMonitor
import org.vicpoo.gasolineproject.domain.connectivity.reconnections
import org.vicpoo.gasolineproject.domain.rbac.PermissionChecker
import org.vicpoo.gasolineproject.domain.rbac.Privileges
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObservePendingVehicleSyncUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.ObserveVehiclesUseCase
import org.vicpoo.gasolineproject.domain.vehicles.usecase.RefreshVehiclesUseCase

data class VehicleListArgs(val mode: VehiclesMode)

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

    private val filter = MutableStateFlow(VehicleFilter())
    /** null = vista de búsqueda cerrada; si no, el borrador de la búsqueda. */
    private val searchDraft = MutableStateFlow<VehicleFilter?>(null)
    private val refreshStatus = MutableStateFlow(RefreshStatus())

    val state: StateFlow<VehicleListUiState> = combine(
        observeVehicles(),
        observePendingSync(),
        permissionChecker.observe(Privileges.CARRO_CREAR),
        combine(filter, searchDraft) { applied, draft -> applied to draft },
        combine(refreshStatus, connectivity.isOnline, lastRefresh.observe(REFRESH_KEY)) { s, online, last -> Triple(s, online, last) },
    ) { vehicles, pending, canAdd, (applied, draft), (status, online, lastUpdated) ->
        VehicleListUiState(
            vehicles = vehicles.filter(applied::matches),
            filter = applied,
            search = draft?.let { d -> VehicleSearchUiState(draft = d, results = vehicles.filter(d::matches)) },
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
            VehicleListEvent.Refresh -> refresh()
            VehicleListEvent.SearchOpened -> searchDraft.value = filter.value
            is VehicleListEvent.QueryChanged -> searchDraft.update { it?.copy(query = event.value) }
            is VehicleListEvent.StatusToggled -> searchDraft.update { it?.copy(statuses = it.statuses.toggle(event.status)) }
            is VehicleListEvent.TypeToggled -> searchDraft.update { it?.copy(types = it.types.toggle(event.type)) }
            VehicleListEvent.DraftCleared -> searchDraft.update { it?.let { VehicleFilter() } }
            VehicleListEvent.SearchDismissed, VehicleListEvent.SearchApplied -> {
                searchDraft.value?.let { filter.value = it.copy(query = it.query.trim()) }
                searchDraft.value = null
            }
            VehicleListEvent.FilterCleared -> filter.value = VehicleFilter()
        }
    }

    private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

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

    private companion object {
        /** Llave de "Actualizado hace X" de esta lista. */
        const val REFRESH_KEY = "vehicles.list"
    }
}
