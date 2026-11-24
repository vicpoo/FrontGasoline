package org.vicpoo.gasolineproject.platform.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.vicpoo.gasolineproject.domain.files.FileCipher.Companion.startsWithMagic
import java.security.KeyStore
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Cifrado real de fotos con el Android Keystore: ./gradlew :platform:connectedAndroidDeviceTest */
@RunWith(AndroidJUnit4::class)
class KeystoreFileCipherTest {

    private val alias = "gasoline_files_test"
    private val cipher = KeystoreFileCipher(alias)
    private val photo = ByteArray(2_000) { (it % 251).toByte() }

    @After
    fun tearDown() {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    @Test
    fun cifra_yDescifraIgual() {
        val stored = cipher.encrypt(photo)

        assertTrue(stored.startsWithMagic())
        assertFalse(stored.copyOfRange(16, stored.size).contentEquals(photo))
        assertContentEquals(photo, cipher.decrypt(stored))
    }

    @Test
    fun dosVecesLaMismaFoto_daArchivosDistintos() {
        assertFalse(cipher.encrypt(photo).contentEquals(cipher.encrypt(photo)))
    }

    @Test
    fun fotoViejaSinCifrar_seRegresaTalCual() {
        assertContentEquals(photo, cipher.decrypt(photo))
        assertFalse(cipher.isEncrypted(photo))
    }

    @Test
    fun archivoAlterado_noSeAcepta() {
        val stored = cipher.encrypt(photo)
        stored[stored.size - 1] = (stored.last() + 1).toByte()

        assertFailsWith<Exception> { cipher.decrypt(stored) }
    }

    @Test
    fun siSePierdeLaLlave_noSePuedeLeer() {
        val stored = cipher.encrypt(photo)
        tearDown()

        assertFailsWith<Exception> { KeystoreFileCipher(alias).decrypt(stored) }
    }
}
