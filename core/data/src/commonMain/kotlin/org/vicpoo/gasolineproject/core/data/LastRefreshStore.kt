package org.vicpoo.gasolineproject.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.core.common.AppClock

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

class DataStoreLastRefreshStore(
    private val dataStore: DataStore<Preferences>,
    private val clock: AppClock,
) : LastRefreshStore {

    override fun observe(key: String): Flow<Long?> =
        dataStore.data.map { it[keyOf(key)] }.distinctUntilChanged()

    override suspend fun markNow(key: String) {
        dataStore.edit { it[keyOf(key)] = clock.nowMillis() }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { it.name.startsWith(PREFIX) }
                .forEach {
                    @Suppress("UNCHECKED_CAST")
                    prefs.remove(it as Preferences.Key<Any>)
                }
        }
    }

    private fun keyOf(key: String) = longPreferencesKey(PREFIX + key)

    private companion object {
        const val PREFIX = "last_refresh."
    }
}
