package org.vicpoo.gasolineproject.ui.vehicles.detail

sealed interface VehicleDetailEffect {
    data object Deleted : VehicleDetailEffect
}
