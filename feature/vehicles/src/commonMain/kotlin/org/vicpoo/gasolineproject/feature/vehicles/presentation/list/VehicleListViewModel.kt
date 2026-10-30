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
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleStatus
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleType
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObservePendingVehicleSyncUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.ObserveVehiclesUseCase
import org.vicpoo.gasolineproject.feature.vehicles.domain.usecase.RefreshVehiclesUseCase

/**
 * Búsqueda + filtros de la lista. Vacío = se ven todos.
 * Los filtros dentro de un grupo se combinan con "o" (Disponible o Mantenimiento) y entre grupos
 * con "y" (Moto y Disponible).
 */
data class VehicleFilter(
    val query: String = "",
    val statuses: Set<VehicleStatus> = emptySet(),
    val types: Set<VehicleType> = emptySet(),
) {
    /** Cuántos filtros hay (la búsqueda por texto no cuenta; va escrita en la barra). */
    val activeFilters: Int get() = statuses.size + types.size

    val isEmpty: Boolean get() = query.isBlank() && activeFilters == 0

    fun matches(vehicle: Vehicle): Boolean {
        if (statuses.isNotEmpty() && vehicle.status !in statuses) return false
        if (types.isNotEmpty() && vehicle.type !in types) return false
        val text = query.trim()
        if (text.isEmpty()) return true
        // Cada palabra debe aparecer en algún dato ("nissan blanco", "abc 123")
        val haystack = listOfNotNull(vehicle.brand, vehicle.model, vehicle.plates, vehicle.color).joinToString(" ")
        return text.split(' ').filter { it.isNotBlank() }.all { word ->
            haystack.contains(word, ignoreCase = true) || vehicle.plates.replace("-", "").contains(word.replace("-", ""), ignoreCase = true)
        }
    }
}

/** Vista de búsqueda abierta (la que se abre al tocar la barra, estilo Instagram / Airbnb). */
data class VehicleSearchUiState(
    /** Lo que se está escribiendo / eligiendo; se aplica a la lista al cerrar la vista. */
    val draft: VehicleFilter = VehicleFilter(),
    /** Vehículos que coinciden con [draft] (para mostrarlos mientras se escribe y contarlos). */
    val results: List<Vehicle> = emptyList(),
)

data class VehicleListUiState(
    val vehicles: List<Vehicle> = emptyList(),
    /** Búsqueda y filtros aplicados a la lista. */
    val filter: VehicleFilter = VehicleFilter(),
    /** null = la vista de búsqueda está cerrada. */
    val search: VehicleSearchUiState? = null,
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
    data object Refresh : VehicleListEvent

    // ---- Vista de búsqueda ----
    /** Tocar la barra: abre la vista con lo que ya estaba aplicado. */
    data object SearchOpened : VehicleListEvent
    /** Regresar (‹ o atrás): sin botón de "Ver resultados", lo elegido queda aplicado al salir. */
    data object SearchDismissed : VehicleListEvent
    data class QueryChanged(val value: String) : VehicleListEvent
    data class StatusToggled(val status: VehicleStatus) : VehicleListEvent
    data class TypeToggled(val type: VehicleType) : VehicleListEvent
    /** "Borrar todo" dentro de la vista. */
    data object DraftCleared : VehicleListEvent
    /** La tecla buscar del teclado: aplica y cierra. */
    data object SearchApplied : VehicleListEvent

    /** La X de la barra: quita búsqueda y filtros sin abrir la vista. */
    data object FilterCleared : VehicleListEvent
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
