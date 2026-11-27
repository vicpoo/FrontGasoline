package org.vicpoo.gasolineproject.ui.triprecording.finish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripField
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripResult
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.FinishTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.RetryFinishTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.StopTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.UndoStopTripUseCase
import org.vicpoo.gasolineproject.ui.triprecording.common.TripFormat

/**
 * @param forLogout true = se abrió desde "Salir" en Perfil: para cerrar sesión hay que cerrar el viaje
 *   con el kilometraje final y la foto. Se guarda como interrumpido por cierre de sesión.
 */
data class FinishTripArgs(val forLogout: Boolean = false)

/**
 * Figma "Registrar Viaje": kilometraje final + foto del odómetro + la ruta que se hizo.
 * Offline-first: al guardar, todo queda en el teléfono y después se intenta enviar.
 * Si se sale sin guardar (flecha o atrás del sistema), el viaje continúa como estaba.
 *
 * Modo salir ([FinishTripArgs.forLogout]): el viaje puede venir en curso o en pausa, así que primero
 * se detiene (como el botón ■). Sin red queda guardado en el teléfono y se envía cuando su dueño
 * vuelva a entrar en este teléfono.
 */
class FinishTripViewModel(
    private val args: FinishTripArgs,
    private val observeTracking: ObserveTrackingUseCase,
    private val finishTrip: FinishTripUseCase,
    private val retryFinish: RetryFinishTripUseCase,
    private val stopTrip: StopTripUseCase,
    private val undoStop: UndoStopTripUseCase,
    private val clock: AppClock,
    private val appScope: AppScope,
) : ViewModel() {

    private val _state = MutableStateFlow(FinishTripUiState(forLogout = args.forLogout))
    val state: StateFlow<FinishTripUiState> = _state.asStateFlow()

    /** true cuando el viaje ya se guardó en el teléfono (enviado o pendiente). */
    private var saved = false

    /**
     * Al salir sin guardar se regresa el viaje a como estaba. En modo salir, si ya estaba detenido
     * (esperando el kilometraje) se queda detenido.
     */
    private var undoOnExit = true

    init {
        if (args.forLogout) {
            viewModelScope.launch {
                val phase = observeTracking().value.trip?.phase
                if (phase == TripPhase.STOPPED) {
                    undoOnExit = false
                } else if (phase != null) {
                    stopTrip()
                }
                load()
            }
        } else {
            load()
        }
    }

    private fun load() {
        val session = observeTracking().value
        val trip = session.trip?.takeIf { it.phase == TripPhase.STOPPED }
        _state.update {
            if (trip == null) {
                it.copy(isLoading = false, notFound = true)
            } else {
                it.copy(
                    isLoading = false,
                    initialMileageText = TripFormat.number(trip.initialMileage),
                    gpsDistanceText = TripFormat.km(trip.distanceMeters),
                    durationText = TripFormat.duration(trip.activeMillis(clock.nowMillis())),
                    route = session.route,
                    // Si el servidor lo rechazó antes, se muestra el kilometraje que ya se había capturado
                    finalMileage = trip.finalMileage?.let(TripFormat::number).orEmpty(),
                )
            }
        }
    }

    fun onEvent(event: FinishTripEvent) {
        when (event) {
            is FinishTripEvent.FinalMileageChanged -> _state.update {
                it.copy(finalMileage = event.value.filterNumber(), errors = it.errors - FinishTripField.FINAL_MILEAGE)
            }
            FinishTripEvent.PhotoClicked -> _state.update { it.copy(showPhotoOptions = true) }
            FinishTripEvent.PhotoOptionsDismissed -> _state.update { it.copy(showPhotoOptions = false) }
            is FinishTripEvent.PhotoPicked -> _state.update {
                if (event.file == null) {
                    it.copy(showPhotoOptions = false)
                } else {
                    it.copy(photo = event.file, showPhotoOptions = false, errors = it.errors - FinishTripField.ODOMETER_PHOTO)
                }
            }
            FinishTripEvent.RemovePhoto -> _state.update { it.copy(photo = null) }
            FinishTripEvent.Save -> save()
            FinishTripEvent.Retry -> retry()
            FinishTripEvent.ResultDismissed -> _state.update {
                // Enviado o guardado sin red: se termina el flujo. Rechazado: se queda para corregir.
                it.copy(outcome = null, failure = null, done = saved)
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true, failure = null) }
        viewModelScope.launch {
            when (val result = finishTrip(current.finalMileage, current.photo, closedByLogout = args.forLogout)) {
                is FinishTripResult.Invalid -> _state.update { it.copy(isSaving = false, errors = result.errors) }
                is FinishTripResult.Failed -> _state.update { it.copy(isSaving = false, failure = result.error) }
                is FinishTripResult.Saved -> {
                    saved = result.outcome !is SendOutcome.Rejected
                    _state.update { it.copy(isSaving = false, outcome = result.outcome) }
                }
            }
        }
    }

    /** "Reintentar" después de guardar sin conexión. */
    private fun retry() {
        _state.update { it.copy(isSaving = true, outcome = null) }
        viewModelScope.launch {
            val outcome = retryFinish()
            _state.update { it.copy(isSaving = false, outcome = outcome) }
        }
    }

    override fun onCleared() {
        // Se salió sin guardar: el viaje sigue (se vuelve a encender el GPS si no estaba en pausa)
        if (!saved && undoOnExit) appScope.scope.launch { undoStop() }
        super.onCleared()
    }

    /** Solo dígitos, un punto y comas de miles ("672,287"). */
    private fun String.filterNumber(): String {
        val filtered = filter { it.isDigit() || it == '.' || it == ',' }
        val firstDot = filtered.indexOf('.')
        return if (firstDot < 0) filtered else filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    }
}
