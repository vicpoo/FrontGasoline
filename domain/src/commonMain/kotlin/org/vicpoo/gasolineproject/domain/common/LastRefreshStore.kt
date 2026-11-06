package org.vicpoo.gasolineproject.domain.common

import kotlinx.coroutines.flow.Flow

/**
 * Cuándo se actualizó por última vez cada lista desde la API, para mostrar
 * "Actualizado hace X" cuando el usuario está viendo la copia guardada sin conexión.
 * Cada pantalla usa su propia llave (ej. "trips.history").
 */
interface LastRefreshStore {
    fun observe(key: String): Flow<Long?>

    /** Se llama cuando la lista se actualizó bien desde la API. */
    suspend fun markNow(key: String)

    /** Al cerrar sesión. */
    suspend fun clear()
}
