package org.vicpoo.gasolineproject.data.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.vicpoo.gasolineproject.domain.session.AuthTokens
import org.vicpoo.gasolineproject.domain.session.TokenStore

/**
 * Android: los tokens se cifran con AES-256-GCM usando una llave que vive en el Android Keystore
 * (hardware seguro cuando el teléfono lo tiene; la llave nunca sale del Keystore).
 * En el DataStore solo queda el texto cifrado (IV + datos + etiqueta GCM) en Base64.
 *
 * No se usa EncryptedSharedPreferences (androidx.security-crypto está deprecado).
 * Si el respaldo de Android restaura el DataStore en otro teléfono, la llave no viaja con él:
 * el descifrado falla, se borra el dato y la app pide iniciar sesión (comportamiento correcto).
 */
class KeystoreTokenStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : TokenStore {

    private val encryptedKey = stringPreferencesKey("auth_tokens_enc")

    override suspend fun read(): AuthTokens? = withContext(Dispatchers.IO) {
        val encoded = dataStore.data.first()[encryptedKey] ?: return@withContext null
        val tokens = runCatching {
            json.decodeFromString(AuthTokens.serializer(), decrypt(encoded))
        }.getOrNull()

        // Llave perdida o dato dañado: no sirve, se borra
        if (tokens == null) dataStore.edit { it.remove(encryptedKey) }
        tokens
    }

    override suspend fun write(tokens: AuthTokens) = withContext(Dispatchers.IO) {
        val encrypted = encrypt(json.encodeToString(AuthTokens.serializer(), tokens))
        dataStore.edit { it[encryptedKey] = encrypted }
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        dataStore.edit { it.remove(encryptedKey) }
        Unit
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.encodeToByteArray())
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val data = Base64.decode(encoded, Base64.NO_WRAP)
        require(data.size > IV_SIZE) { "Token cifrado inválido" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_SIZE_BITS, data, 0, IV_SIZE))
        return cipher.doFinal(data, IV_SIZE, data.size - IV_SIZE).decodeToString()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "gasoline_auth_tokens_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
    }
}
