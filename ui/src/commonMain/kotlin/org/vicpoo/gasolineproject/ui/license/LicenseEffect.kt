package org.vicpoo.gasolineproject.ui.license

sealed interface LicenseEffect {
    data object GoToHome : LicenseEffect
}
