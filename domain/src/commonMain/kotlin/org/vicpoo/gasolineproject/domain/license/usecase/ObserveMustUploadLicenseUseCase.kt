package org.vicpoo.gasolineproject.domain.license.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.license.mustUploadLicense
import org.vicpoo.gasolineproject.domain.session.SessionReader

/** true si el usuario maneja (viaje.registrar) y aún no sube su licencia (cambia si le dan el privilegio). */
class ObserveMustUploadLicenseUseCase(private val sessionReader: SessionReader) {
    operator fun invoke(): Flow<Boolean> = sessionReader.session.map { it?.mustUploadLicense == true }.distinctUntilChanged()
}
