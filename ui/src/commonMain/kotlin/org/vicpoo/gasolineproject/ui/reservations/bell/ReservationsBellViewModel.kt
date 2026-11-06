package org.vicpoo.gasolineproject.ui.reservations.bell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.push.PushEvents
import org.vicpoo.gasolineproject.domain.push.PushType
import org.vicpoo.gasolineproject.domain.reservations.ObserveUnseenReservationUpdatesUseCase
import org.vicpoo.gasolineproject.domain.reservations.RefreshUnseenReservationUpdatesUseCase

/** Número del badge: respuestas a mis solicitudes que no he visto (sin red, el último guardado). */
class ReservationsBellViewModel(
    observeUnseen: ObserveUnseenReservationUpdatesUseCase,
    private val refreshUnseen: RefreshUnseenReservationUpdatesUseCase,
    pushEvents: PushEvents,
) : ViewModel() {

    val unseen: StateFlow<Int> = observeUnseen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        refresh()
        // El número de la campana sube en cuanto llega la respuesta
        viewModelScope.launch {
            pushEvents.received.collect { if (it.type == PushType.REQUEST_RESULT) refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshUnseen() }
    }
}
