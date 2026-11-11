package org.vicpoo.gasolineproject.domain.settings

import kotlinx.coroutines.flow.Flow

/** Apariencia de la app que el usuario elige en Perfil. */
enum class ThemeMode {
    /** Igual que el teléfono (por defecto). */
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Dónde se guarda la apariencia (lo implementa data con DataStore).
 * Es del teléfono, no de la cuenta: NO se borra al cerrar sesión.
 */
interface ThemePreferenceStore {
    fun observe(): Flow<ThemeMode>

    suspend fun save(mode: ThemeMode)
}

class ObserveThemeModeUseCase(private val store: ThemePreferenceStore) {
    operator fun invoke(): Flow<ThemeMode> = store.observe()
}

class SetThemeModeUseCase(private val store: ThemePreferenceStore) {
    suspend operator fun invoke(mode: ThemeMode) = store.save(mode)
}
