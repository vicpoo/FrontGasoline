package org.vicpoo.gasolineproject.platform.database

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Contraseña de la base local (32 bytes al azar), guardada en [file] cifrada con AES-256-GCM usando
 * una llave del Android Keystore ([keyAlias]). La llave nunca sale del Keystore; en el archivo solo
 * queda IV + texto cifrado + etiqueta GCM.
 *
 * El archivo va en noBackupFilesDir: si alguien copia la base a otro teléfono, sin esta llave no abre.
 */
internal class DatabasePassphraseStore(
    private val file: File,
    private val keyAlias: String = KEY_ALIAS,
) {
    fun exists(): Boolean = file.exists()

    /** La contraseña guardada, o null si no hay o ya no se puede descifrar (llave perdida o archivo dañado). */
    fun read(): ByteArray? {
        if (!file.exists()) return null
        return runCatching {
            val data = file.readBytes()
            require(data.size > IV_SIZE) { "Archivo de llave inválido" }
            val key = existingKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, data, 0, IV_SIZE))
            cipher.doFinal(data, IV_SIZE, data.size - IV_SIZE)
        }.getOrNull()
    }

    /** Genera una contraseña nueva, la guarda cifrada (escritura atómica) y la regresa. */
    fun createNew(): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: generateKey())
        val encrypted = cipher.iv + cipher.doFinal(passphrase)

        file.parentFile?.mkdirs()
        val temp = File(file.path + ".tmp")
        temp.writeBytes(encrypted)
        if (!temp.renameTo(file)) {
            file.delete()
            check(temp.renameTo(file)) { "No se pudo guardar la llave de la base local" }
        }
        return passphrase
    }

    /** Borra el archivo y la llave del Keystore (la siguiente contraseña usa una llave nueva). */
    fun delete() {
        file.delete()
        File(file.path + ".tmp").delete()
        runCatching { keyStore().deleteEntry(keyAlias) }
    }

    private fun existingKey(): SecretKey? =
        (keyStore().getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        const val KEY_ALIAS = "gasoline_local_db_v1"
        const val PASSPHRASE_SIZE = 32
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val IV_SIZE = 12
        private const val TAG_SIZE_BITS = 128
    }
}
