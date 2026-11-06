package org.vicpoo.gasolineproject.ui.license

import org.vicpoo.gasolineproject.domain.files.PickedFile

sealed interface LicenseEvent {
    data class FilePicked(val file: PickedFile?) : LicenseEvent
    data object Retry : LicenseEvent
    data object UploadClicked : LicenseEvent
    data object SourceOptionsDismissed : LicenseEvent
}
