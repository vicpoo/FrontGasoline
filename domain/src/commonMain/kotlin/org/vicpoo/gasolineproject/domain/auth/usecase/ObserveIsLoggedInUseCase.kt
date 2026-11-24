package org.vicpoo.gasolineproject.domain.auth.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.session.SessionReader

/** true mientras haya sesión; cambia a false al cerrar sesión o si la API rechazó el refresh token. */
class ObserveIsLoggedInUseCase(private val sessionReader: SessionReader) {
    operator fun invoke(): Flow<Boolean> = sessionReader.session.map { it != null }.distinctUntilChanged()
}
