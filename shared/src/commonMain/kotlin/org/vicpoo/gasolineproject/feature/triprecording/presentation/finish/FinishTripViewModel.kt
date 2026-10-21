package org.vicpoo.gasolineproject.feature.triprecording.presentation.finish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppClock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.di.AppScope
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.feature.triprecording.domain.FinishTripField
import org.vicpoo.gasolineproject.feature.triprecording.domain.FinishTripResult
import org.vicpoo.gasolineproject.feature.triprecording.domain.FinishTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RetryFinishTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.SendOutcome
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripPhase
import org.vicpoo.gasolineproject.feature.triprecording.domain.UndoStopTripUseCase
import org.vicpoo.gasolineproject.feature.triprecording.presentation.common.TripFormat

data class FinishTripUiState(
    val isLoading: Boolean = true,
    /** No hay viaje detenido esperando el kilometraje (ej. ya se guardó). */
    val notFound: Boolean = false,
    val initialMileageText: String = "",
    val gpsDistanceText: String = "0",
    val durationText: String = "",
    val route: List<GeoPoint> = emptyList(),
    val finalMileage: String = "",
    val photo: PickedFile? = null,
    val errors: Map<FinishTripField, ValidationReason> = emptyMap(),
    val showPhotoOptions: Boolean = false,
    val isSaving: Boolean = false,
    val outcome: SendOutcome? = null,
    val failure: AppError? = null,
    /** true → regresar a la lista de carros (el viaje ya quedó guardado). */
    val done: Boolean = false,
)

sealed interface FinishTripEvent {
    data class FinalMileageChanged(val value: String) : FinishTripEvent
    data object PhotoClicked : FinishTripEvent
    data object PhotoOptionsDismissed : FinishTripEvent
    data class PhotoPicked(val file: PickedFile?) : FinishTripEvent
    data object RemovePhoto : FinishTripEvent
    data object Save : FinishTripEvent
    data object Retry : FinishTripEvent
    /** Cierra el diálogo de resultado. */
    data object ResultDismissed : FinishTripEvent
}

/**
 * Figma "Registrar Viaje": kilometraje final + foto del odómetro + la ruta que se hizo.
 * Offline-first: al guardar, todo queda en el teléfono y después se intenta enviar.
 * Si se sale sin guardar (flecha o atrás del sistema), el viaje continúa como estaba.
 */
class FinishTripViewModel(
    private val observeTracking: ObserveTrackingUseCase,
    private val finishTrip: FinishTripUseCase,
    private val retryFinish: RetryFinishTripUseCase,
    private val undoStop: UndoStopTripUseCase,
    private val clock: AppClock,
    private val appScope: AppScope,
) : ViewModel() {

    private val _state = MutableStateFlow(FinishTripUiState())
    val state: StateFlow<FinishTripUiState> = _state.asStateFlow()

    /** true cuando el viaje ya se guardó en el teléfono (enviado o pendiente). */
    private var saved = false

    init {
        load()
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
            when (val result = finishTrip(current.finalMileage, current.photo)) {
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
        if (!saved) appScope.scope.launch { undoStop() }
        super.onCleared()
    }

    /** Solo dígitos, un punto y comas de miles ("672,287"). */
    private fun String.filterNumber(): String {
        val filtered = filter { it.isDigit() || it == '.' || it == ',' }
        val firstDot = filtered.indexOf('.')
        return if (firstDot < 0) filtered else filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    }
}
