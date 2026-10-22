package org.vicpoo.gasolineproject.core.startup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Avisa cuándo la app ya sabe a qué pantalla entrar (login o pestañas).
 * Android deja el logo de inicio del sistema en pantalla hasta que esto es true
 * (splashScreen.setKeepOnScreenCondition en MainActivity), así solo se ve UN logo.
 */
object AppStartup {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun markReady() {
        _isReady.value = true
    }
}
