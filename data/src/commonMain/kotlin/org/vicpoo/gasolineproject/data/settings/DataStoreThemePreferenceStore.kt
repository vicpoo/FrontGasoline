package org.vicpoo.gasolineproject.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.settings.ThemeMode
import org.vicpoo.gasolineproject.domain.settings.ThemePreferenceStore

/**
 * [ThemePreferenceStore] en el DataStore de la app. Se guarda el nombre del modo ("LIGHT", "DARK"...);
 * si no hay nada guardado (o es un valor desconocido) se usa [ThemeMode.SYSTEM].
 */
class DataStoreThemePreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferenceStore {

    override fun observe(): Flow<ThemeMode> = dataStore.data
        .map { prefs -> prefs[THEME_MODE].toThemeMode() }
        .distinctUntilChanged()

    override suspend fun save(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    private fun String?.toThemeMode(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
