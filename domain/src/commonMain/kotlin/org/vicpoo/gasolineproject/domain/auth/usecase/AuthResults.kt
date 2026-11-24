package org.vicpoo.gasolineproject.domain.auth.usecase

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.session.Session

/** Resultado de iniciar sesión. */
sealed interface LoginResult {
    data class Success(val session: Session) : LoginResult

    data class Failure(val error: AppError) : LoginResult

    /**
     * En el teléfono hay [pendingCount] registros sin enviar de otra cuenta ([ownerEmail]).
     * Entrar con esta cuenta los borra (los viajes NO: se guardan aparte para esa cuenta): la pantalla pregunta y, si el usuario acepta, se vuelve a
     * llamar con discardOtherAccountData = true.
     */
    data class OtherAccountPendingData(val ownerEmail: String, val pendingCount: Int) : LoginResult
}

/** En qué estado está el usuario al abrir la app. */
enum class AuthStatus { LoggedOut, NeedsLicense, LoggedIn }

