package org.vicpoo.gasolineproject.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.LastRefreshStore

/** [LastRefreshStore] guardado en DataStore (una llave por lista). */
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
