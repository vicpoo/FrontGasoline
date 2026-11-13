package org.vicpoo.gasolineproject.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.vicpoo.gasolineproject.domain.settings.ThemeMode
import org.vicpoo.gasolineproject.domain.settings.ThemePreferenceStore

/**
 * [ThemePreferenceStore] solo en memoria: la app SIEMPRE abre con el modo del teléfono ([ThemeMode.SYSTEM]).
 * El botón de Perfil cambia el modo mientras la app está abierta; al volver a abrirla (o si el teléfono
 * cambia de modo) se vuelve a seguir al teléfono. No se guarda nada en disco a propósito.
 */
class SessionThemePreferenceStore : ThemePreferenceStore {

    private val mode = MutableStateFlow(ThemeMode.SYSTEM)

    override fun observe(): Flow<ThemeMode> = mode.asStateFlow()

    override suspend fun save(mode: ThemeMode) {
        this.mode.value = mode
    }
}
