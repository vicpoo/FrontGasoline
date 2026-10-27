package org.vicpoo.gasolineproject.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

const val DATASTORE_FILE_NAME = "gasoline.preferences_pb"

/** Crea el DataStore; cada plataforma indica la ruta del archivo (ver platformModule). */
fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

/**
 * Sesión persistida con DataStore como JSON.
 * TODO (seguridad): en producción cifrar el token (EncryptedFile / Keychain).
 */
class DataStoreSessionStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : SessionStore {

    private val sessionKey = stringPreferencesKey("session_json")

    override val session: Flow<Session?> = dataStore.data.map { prefs ->
        prefs[sessionKey]?.let { raw -> runCatching { json.decodeFromString(Session.serializer(), raw) }.getOrNull() }
    }

    override suspend fun current(): Session? = session.first()

    override suspend fun save(session: Session) {
        dataStore.edit { it[sessionKey] = json.encodeToString(Session.serializer(), session) }
    }

    override suspend fun markLicenseUploaded() {
        val updated = current()?.copy(requiresLicense = false) ?: return
        save(updated)
    }

    override suspend fun updatePrivileges(privileges: Set<String>, version: String?) {
        val updated = current()?.copy(privileges = privileges, privilegesVersion = version) ?: return
        save(updated)
    }

    override suspend fun update(transform: (Session) -> Session) {
        val updated = current()?.let(transform) ?: return
        save(updated)
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(sessionKey) }
    }
}
