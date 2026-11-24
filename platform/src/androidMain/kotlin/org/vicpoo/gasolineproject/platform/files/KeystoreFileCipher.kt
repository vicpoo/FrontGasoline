package org.vicpoo.gasolineproject.platform.files

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.vicpoo.gasolineproject.domain.files.FileCipher
import org.vicpoo.gasolineproject.domain.files.FileCipher.Companion.MAGIC
import org.vicpoo.gasolineproject.domain.files.FileCipher.Companion.startsWithMagic
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android: fotos pendientes cifradas con AES-256-GCM y una llave del Android Keystore ([keyAlias]).
 * Formato del archivo: "GPF1" + IV (12 bytes) + datos cifrados + etiqueta GCM (16 bytes).
 * Si la llave se pierde (restablecer el teléfono), la foto ya no se puede leer: el envío la reporta
 * como faltante, igual que si el archivo no existiera.
 */
class KeystoreFileCipher(private val keyAlias: String = KEY_ALIAS) : FileCipher {

    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: generateKey())
        return MAGIC + cipher.iv + cipher.doFinal(plain)
    }

    override fun decrypt(stored: ByteArray): ByteArray {
        if (!stored.startsWithMagic()) return stored // foto guardada antes de esta versión
        val start = MAGIC.size
        require(stored.size > start + IV_SIZE) { "Archivo cifrado incompleto" }
        val key = checkNotNull(existingKey()) { "No existe la llave para descifrar las fotos" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, stored, start, IV_SIZE))
        return cipher.doFinal(stored, start + IV_SIZE, stored.size - start - IV_SIZE)
    }

    private fun existingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    @Synchronized
    private fun generateKey(): SecretKey {
        existingKey()?.let { return it }
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

    companion object {
        const val KEY_ALIAS = "gasoline_files_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val IV_SIZE = 12
        private const val TAG_SIZE_BITS = 128
    }
}
