package org.vicpoo.gasolineproject.ui.trips.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.trips.LoadTripDetailsUseCase
import org.vicpoo.gasolineproject.domain.trips.ObserveTripUseCase

data class TripDetailArgs(val tripId: String)

class TripDetailViewModel(
    args: TripDetailArgs,
    observeTrip: ObserveTripUseCase,
    loadTripDetails: LoadTripDetailsUseCase,
) : ViewModel() {

    init {
        // Si se abre un viaje cuyas cargas y ruta aún no se habían descargado (no se había llegado a él en la lista)
        viewModelScope.launch { loadTripDetails(listOf(args.tripId)) }
    }

    private val openReceipt = MutableStateFlow<String?>(null)

    val state: StateFlow<TripDetailUiState> = combine(observeTrip(args.tripId), openReceipt) { trip, receipt ->
        TripDetailUiState(trip = trip, isLoading = false, openReceiptUrl = receipt)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripDetailUiState())

    fun openReceipt(url: String?) {
        openReceipt.value = url
    }

    fun closeReceipt() {
        openReceipt.value = null
    }
}
