package org.vicpoo.gasolineproject.feature.triprecording.presentation.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenter
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelField
import org.vicpoo.gasolineproject.feature.triprecording.domain.FuelRecordDraft
import org.vicpoo.gasolineproject.feature.triprecording.domain.GetFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveCostCentersUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.RetryFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.SaveFuelRecordUseCase
import org.vicpoo.gasolineproject.feature.triprecording.domain.SaveFuelResult
import org.vicpoo.gasolineproject.feature.triprecording.domain.SendOutcome
import org.vicpoo.gasolineproject.feature.triprecording.presentation.common.TripFormat
import org.vicpoo.gasolineproject.feature.triprecording.presentation.common.toImageModel

/** recordId == null -> Agregar; != null -> Editar. */
data class FuelRecordFormArgs(val recordId: String?)

data class FuelRecordFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val amount: String = "",
    val liters: String = "",
    val costCenters: List<CostCenter> = emptyList(),
    val selectedCostCenterId: String? = null,
    /** Lo que se muestra en la vista previa (bytes nuevos, archivo local o URL). */
    val receiptPreview: Any? = null,
    val newReceipt: PickedFile? = null,
    val hasCurrentReceipt: Boolean = false,
    val tankCapacityText: String? = null,
    val errors: Map<FuelField, ValidationReason> = emptyMap(),
    val showReceiptOptions: Boolean = false,
    val isSaving: Boolean = false,
    /** Registro guardado (para "Reintentar"). */
    val savedRecordId: String? = null,
    val outcome: SendOutcome? = null,
    val failure: AppError? = null,
)

sealed interface FuelRecordFormEvent {
    data class AmountChanged(val value: String) : FuelRecordFormEvent
    data class LitersChanged(val value: String) : FuelRecordFormEvent
    data class CostCenterSelected(val id: String) : FuelRecordFormEvent
    data object UploadReceiptClicked : FuelRecordFormEvent
    data object ReceiptOptionsDismissed : FuelRecordFormEvent
    data class ReceiptPicked(val file: PickedFile?) : FuelRecordFormEvent
    data object RemoveReceipt : FuelRecordFormEvent
    data object Save : FuelRecordFormEvent
    data object Retry : FuelRecordFormEvent
    data object ResultDismissed : FuelRecordFormEvent
}

/** Figma "Agregar / Editar Registro de gasolina". */
class FuelRecordFormViewModel(
    private val args: FuelRecordFormArgs,
    private val getRecord: GetFuelRecordUseCase,
    private val observeCostCenters: ObserveCostCentersUseCase,
    private val observeTracking: ObserveTrackingUseCase,
    private val saveRecord: SaveFuelRecordUseCase,
    private val retryRecord: RetryFuelRecordUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(FuelRecordFormUiState(isEdit = args.recordId != null))
    val state: StateFlow<FuelRecordFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val costCenters = observeCostCenters().first()
        val trip = observeTracking().value.trip
        val tank = trip?.vehicle?.tankCapacityLiters?.takeIf { it > 0 }?.let { TripFormat.number(it) }
        val recordId = args.recordId
        if (recordId == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    costCenters = costCenters,
                    // Por defecto, el centro de costos con el que se inició el viaje
                    selectedCostCenterId = trip?.costCenter?.id?.takeIf { id -> costCenters.any { c -> c.id == id } }
                        ?: costCenters.singleOrNull()?.id,
                    tankCapacityText = tank,
                    notFound = trip == null,
                )
            }
            return
        }
        val record = getRecord(recordId)
        _state.update {
            if (record == null) {
                it.copy(isLoading = false, notFound = true)
            } else {
                it.copy(
                    isLoading = false,
                    amount = record.amount?.let(TripFormat::number).orEmpty(),
                    liters = TripFormat.number(record.liters),
                    costCenters = costCenters,
                    selectedCostCenterId = record.costCenter?.id,
                    receiptPreview = record.receipt?.toImageModel(),
                    hasCurrentReceipt = record.receipt != null,
                    tankCapacityText = tank,
                )
            }
        }
    }

    fun onEvent(event: FuelRecordFormEvent) {
        when (event) {
            is FuelRecordFormEvent.AmountChanged -> _state.update {
                it.copy(amount = event.value.filterNumber(), errors = it.errors - FuelField.AMOUNT)
            }
            is FuelRecordFormEvent.LitersChanged -> _state.update {
                it.copy(liters = event.value.filterNumber(), errors = it.errors - FuelField.LITERS)
            }
            is FuelRecordFormEvent.CostCenterSelected -> _state.update {
                it.copy(selectedCostCenterId = event.id, errors = it.errors - FuelField.COST_CENTER)
            }
            FuelRecordFormEvent.UploadReceiptClicked -> _state.update { it.copy(showReceiptOptions = true) }
            FuelRecordFormEvent.ReceiptOptionsDismissed -> _state.update { it.copy(showReceiptOptions = false) }
            is FuelRecordFormEvent.ReceiptPicked -> event.file?.let { file ->
                _state.update {
                    it.copy(
                        newReceipt = file,
                        receiptPreview = file.bytes,
                        showReceiptOptions = false,
                        errors = it.errors - FuelField.RECEIPT,
                    )
                }
            } ?: _state.update { it.copy(showReceiptOptions = false) }
            FuelRecordFormEvent.RemoveReceipt -> _state.update {
                it.copy(newReceipt = null, receiptPreview = null, hasCurrentReceipt = false)
            }
            FuelRecordFormEvent.Save -> save()
            FuelRecordFormEvent.Retry -> retry()
            FuelRecordFormEvent.ResultDismissed -> _state.update { it.copy(outcome = null, failure = null) }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true, failure = null) }
        viewModelScope.launch {
            val draft = FuelRecordDraft(
                localId = current.savedRecordId ?: args.recordId,
                amountText = current.amount,
                litersText = current.liters,
                costCenterId = current.selectedCostCenterId,
                newReceipt = current.newReceipt,
                hasCurrentReceipt = current.hasCurrentReceipt,
            )
            when (val result = saveRecord(draft)) {
                is SaveFuelResult.Invalid -> _state.update { it.copy(isSaving = false, errors = result.errors) }
                is SaveFuelResult.Failed -> _state.update { it.copy(isSaving = false, failure = result.error) }
                is SaveFuelResult.Saved -> _state.update {
                    it.copy(
                        isSaving = false,
                        outcome = result.outcome,
                        // Ya quedó guardado: si se reintenta no se duplica
                        savedRecordId = result.recordId,
                        newReceipt = null,
                        hasCurrentReceipt = true,
                    )
                }
            }
        }
    }

    private fun retry() {
        val recordId = _state.value.savedRecordId ?: args.recordId ?: return save()
        _state.update { it.copy(isSaving = true, outcome = null) }
        viewModelScope.launch {
            val outcome = retryRecord(recordId)
            _state.update { it.copy(isSaving = false, outcome = outcome) }
        }
    }

    /** Solo dígitos, un punto y comas de miles. */
    private fun String.filterNumber(): String {
        val filtered = filter { it.isDigit() || it == '.' || it == ',' }
        val firstDot = filtered.indexOf('.')
        return if (firstDot < 0) filtered else filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    }
}
