package org.vicpoo.gasolineproject.ui.vehicles.form

sealed interface VehicleFormEffect {
    data object Saved : VehicleFormEffect
    data object Close : VehicleFormEffect
}
