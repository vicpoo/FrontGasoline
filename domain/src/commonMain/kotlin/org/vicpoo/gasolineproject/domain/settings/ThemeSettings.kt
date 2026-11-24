package org.vicpoo.gasolineproject.domain.settings

import kotlinx.coroutines.flow.Flow

/** Apariencia de la app: igual que el teléfono ([SYSTEM]) hasta que el usuario elige otra con el botón de Perfil. */
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

