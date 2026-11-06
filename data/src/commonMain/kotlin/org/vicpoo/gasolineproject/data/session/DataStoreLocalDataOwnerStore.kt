package org.vicpoo.gasolineproject.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import org.vicpoo.gasolineproject.domain.session.LocalDataOwner
import org.vicpoo.gasolineproject.domain.session.LocalDataOwnerStore

/**
 * [LocalDataOwnerStore] en el DataStore de la app. NO se borra cuando la sesión termina sola
 * (token vencido o revocado): solo al cerrar sesión, que también borra los datos del teléfono.
 */
class DataStoreLocalDataOwnerStore(
    private val dataStore: DataStore<Preferences>,
) : LocalDataOwnerStore {

    override suspend fun current(): LocalDataOwner? {
        val prefs = dataStore.data.first()
        val userId = prefs[USER_ID] ?: return null
        val email = prefs[EMAIL] ?: return null
        return LocalDataOwner(userId = userId, email = email)
    }

    override suspend fun save(owner: LocalDataOwner) {
        dataStore.edit {
            it[USER_ID] = owner.userId
            it[EMAIL] = owner.email
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(USER_ID)
            it.remove(EMAIL)
        }
    }

    private companion object {
        val USER_ID = stringPreferencesKey("local_data_owner_user_id")
        val EMAIL = stringPreferencesKey("local_data_owner_email")
    }
}
