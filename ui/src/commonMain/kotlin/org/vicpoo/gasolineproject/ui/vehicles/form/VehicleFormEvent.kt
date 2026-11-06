package org.vicpoo.gasolineproject.ui.vehicles.form

import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.vehicles.model.VehicleType
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleField

sealed interface VehicleFormEvent {
    /** "Agregar" / "Modificar" de una fila: abre la hoja de esa sección. */
    data class OpenSection(val section: VehicleFormSection) : VehicleFormEvent
    data object CancelSection : VehicleFormEvent
    data object ConfirmSection : VehicleFormEvent

    data class FieldChanged(val field: VehicleField, val value: String) : VehicleFormEvent
    /** Carro / Moto / Camioneta / Camión (tarjetas en la hoja "Vehículo"). */
    data class TypeChanged(val type: VehicleType) : VehicleFormEvent
    data class PhotoPicked(val file: PickedFile?) : VehicleFormEvent
    data object RemovePhoto : VehicleFormEvent

    data object Save : VehicleFormEvent
}
