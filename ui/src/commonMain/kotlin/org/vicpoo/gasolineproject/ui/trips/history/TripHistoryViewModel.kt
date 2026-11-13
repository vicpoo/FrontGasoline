package org.vicpoo.gasolineproject.ui.trips.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.LastRefreshStore
import org.vicpoo.gasolineproject.domain.connectivity.ConnectivityMonitor
import org.vicpoo.gasolineproject.domain.connectivity.reconnections
import org.vicpoo.gasolineproject.domain.trips.LoadMoreTripsUseCase
import org.vicpoo.gasolineproject.domain.trips.LoadTripDetailsUseCase
import org.vicpoo.gasolineproject.domain.trips.ObserveTripsUseCase
import org.vicpoo.gasolineproject.domain.trips.RefreshTripsUseCase

class TripHistoryViewModel(
    private val observeTrips: ObserveTripsUseCase,
    private val refreshTrips: RefreshTripsUseCase,
    private val loadTripDetails: LoadTripDetailsUseCase,
    /** Pide a la API los 10 viajes siguientes cuando el usuario llega al final. */
    private val loadMoreTrips: LoadMoreTripsUseCase,
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

    /** Hay una petición de "los 10 siguientes" en curso. */
    private val loadingMore = MutableStateFlow(false)

    /** La API tiene más viajes después de los guardados. */
    private val remoteHasMore = loadMoreTrips.hasMore().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val state: StateFlow<TripHistoryUiState> = combine(
        combine(allTrips, visibleCount, remoteHasMore, ::Triple),
        status,
        connectivity.isOnline,
        lastRefresh.observe(REFRESH_KEY),
    ) { (trips, visible, moreInApi), s, online, lastUpdated ->
        TripHistoryUiState(
            trips = trips.take(visible),
            hasMore = trips.size > visible || moreInApi,
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
     * El usuario llegó al final de lo que se ve: se muestran 10 más y se piden sus cargas y su ruta.
     * Si ya se mostraron todos los guardados, primero se piden los 10 siguientes a la API.
     * Así no se descarga TODO el historial (ni sus mapas y cargas) de golpe.
     */
    fun loadMore() {
        val current = visibleCount.value
        if (allTrips.value.size > current) {
            reveal(current, allTrips.value)
            return
        }
        if (!remoteHasMore.value || loadingMore.value) return
        loadingMore.value = true
        viewModelScope.launch {
            val failure = (loadMoreTrips() as? AppResult.Failure)?.error
            if (failure != null) {
                status.update {
                    it.copy(isOffline = failure == AppError.NoConnection, error = failure.takeIf { e -> e != AppError.NoConnection })
                }
            }
            // Lectura directa del caché: ya trae los que se acaban de agregar
            reveal(current, observeTrips().first())
            loadingMore.value = false
        }
    }

    private fun reveal(current: Int, trips: List<org.vicpoo.gasolineproject.domain.trips.Trip>) {
        if (trips.size <= current) return
        val next = (current + PAGE_SIZE).coerceAtMost(trips.size)
        visibleCount.value = next
        loadDetailsOf(trips.subList(current, next).map { it.id })
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

        /** Viajes por bloque: se muestran los 10 más recientes y al bajar se agregan 10 más. */
        const val PAGE_SIZE = 10
    }
}
