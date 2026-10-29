package org.vicpoo.gasolineproject.feature.trips.presentation.history

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
import org.vicpoo.gasolineproject.feature.trips.domain.LoadTripDetailsUseCase
import org.vicpoo.gasolineproject.feature.trips.domain.ObserveTripsUseCase
import org.vicpoo.gasolineproject.feature.trips.domain.RefreshTripsUseCase
import org.vicpoo.gasolineproject.feature.trips.domain.Trip

data class TripHistoryUiState(
    /** Solo los viajes que ya se muestran (se agregan de 4 en 4 al bajar). */
    val trips: List<Trip> = emptyList(),
    /** Faltan viajes por mostrar (se muestra el indicador al final de la lista). */
    val hasMore: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: AppError? = null,
    /** Cuándo se trajo de la API la copia que se ve (para "Actualizado hace X"). */
    val lastUpdatedAtMillis: Long? = null,
)

class TripHistoryViewModel(
    observeTrips: ObserveTripsUseCase,
    private val refreshTrips: RefreshTripsUseCase,
    private val loadTripDetails: LoadTripDetailsUseCase,
    /** Se recarga sola al volver la red y marca "sin conexión" en cuanto se pierde. */
    private val connectivity: ConnectivityMonitor,
    private val lastRefresh: LastRefreshStore,
) : ViewModel() {

    private data class Status(
        val loadedOnce: Boolean = false,
        /** Hay una petición en curso (visible o en segundo plano). */
        val isLoading: Boolean = false,
        /** Solo true cuando el usuario jaló para recargar: es lo único que muestra el indicador. */
        val isRefreshing: Boolean = false,
        val isOffline: Boolean = false,
        val error: AppError? = null,
    )

    private val status = MutableStateFlow(Status())

    /** Cuántos viajes se muestran: empieza en [PAGE_SIZE] y crece de [PAGE_SIZE] en [PAGE_SIZE]. */
    private val visibleCount = MutableStateFlow(PAGE_SIZE)

    /** Todos los viajes guardados (para saber qué ids pedir al mostrar el siguiente bloque). */
    private val allTrips = observeTrips().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<TripHistoryUiState> = combine(
        allTrips,
        visibleCount,
        status,
        connectivity.isOnline,
        lastRefresh.observe(REFRESH_KEY),
    ) { trips, visible, s, online, lastUpdated ->
        TripHistoryUiState(
            trips = trips.take(visible),
            hasMore = trips.size > visible,
            isInitialLoading = trips.isEmpty() && !s.loadedOnce,
            isRefreshing = s.isRefreshing,
            isOffline = s.isOffline || !online,
            error = s.error,
            lastUpdatedAtMillis = lastUpdated,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripHistoryUiState())

    init {
        refreshSilently()
        viewModelScope.launch { connectivity.reconnections().collect { refreshSilently() } }
    }

    /**
     * El usuario llegó al final de lo que se ve: se muestran 4 más y se piden sus cargas y su ruta.
     * Así no se descargan los mapas y cargas de TODO el historial de golpe.
     */
    fun loadMore() {
        val total = allTrips.value.size
        val current = visibleCount.value
        if (current >= total) return
        val next = (current + PAGE_SIZE).coerceAtMost(total)
        visibleCount.value = next
        loadDetailsOf(allTrips.value.subList(current, next).map { it.id })
    }

    private fun loadDetailsOf(ids: List<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { loadTripDetails(ids) }
    }

    /** El usuario jaló para recargar: se muestra el indicador. */
    fun refresh() = load(showIndicator = true)

    /** Al abrir o volver a la pestaña: se actualiza sin mostrar el indicador. */
    fun refreshSilently() = load(showIndicator = false)

    private fun load(showIndicator: Boolean) {
        if (status.value.isLoading) {
            // Si ya hay una carga silenciosa y el usuario jala, se muestra el indicador hasta que termine
            if (showIndicator) status.update { it.copy(isRefreshing = true) }
            return
        }
        viewModelScope.launch {
            status.update { it.copy(isLoading = true, isRefreshing = showIndicator) }
            val failure = (refreshTrips() as? AppResult.Failure)?.error
            if (failure == null) {
                lastRefresh.markNow(REFRESH_KEY)
                // Detalles solo de lo que ya se ve (los primeros 4, o más si el usuario ya bajó)
                loadDetailsOf(allTrips.value.take(visibleCount.value).map { it.id })
            }
            status.value = Status(
                loadedOnce = true,
                isOffline = failure == AppError.NoConnection,
                error = failure?.takeIf { it != AppError.NoConnection },
            )
        }
    }

    private companion object {
        /** Llave de "Actualizado hace X" de esta lista. */
        const val REFRESH_KEY = "trips.history"

        /** Viajes por bloque: se muestran 4 y al bajar se agregan 4 más. */
        const val PAGE_SIZE = 4
    }
}
