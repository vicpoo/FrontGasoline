package org.vicpoo.gasolineproject.feature.auth.presentation.license

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.ObserveMustUploadLicenseUseCase

/**
 * Estado de la puerta de licencia ([RequireLicense]): null mientras se lee la sesión,
 * true si el usuario maneja y todavía no sube su licencia.
 */
class LicenseGateViewModel(
    observeMustUploadLicense: ObserveMustUploadLicenseUseCase,
) : ViewModel() {

    val mustUploadLicense: StateFlow<Boolean?> = observeMustUploadLicense()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
