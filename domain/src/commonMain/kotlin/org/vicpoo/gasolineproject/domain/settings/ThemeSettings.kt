package org.vicpoo.gasolineproject.domain.settings

import kotlinx.coroutines.flow.Flow

/** Apariencia de la app. Siempre arranca igual que el teléfono; el botón de Perfil la cambia mientras la app está abierta. */
enum class ThemeMode {
    /** Igual que el teléfono (por defecto). */
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Dónde vive la apariencia (en data: solo en memoria, así cada vez que se abre la app sigue al teléfono).
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
