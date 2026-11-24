package org.vicpoo.gasolineproject.domain.common

/**
 * Almacén de textos pequeños por clave. Lo implementa data con DataStore.
 *
 * Existe para que platform guarde datos ya cifrados (los tokens en Android, ver KeystoreTokenStore)
 * sin depender de DataStore: platform pone el cifrado (Keystore) y data el almacenamiento.
 */
interface KeyValueStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)
}
