package org.vicpoo.gasolineproject.testing

import org.vicpoo.gasolineproject.domain.files.FileCipher
import org.vicpoo.gasolineproject.domain.files.FileCipher.Companion.MAGIC
import org.vicpoo.gasolineproject.domain.files.FileCipher.Companion.startsWithMagic

/**
 * "Cifrado" para pruebas: el encabezado real ("GPF1") + los bytes invertidos con XOR. No protege
 * nada, pero el archivo deja de ser igual a la foto, así que las pruebas pueden comprobar que en el
 * disco no queda la foto en claro y que al leer se recupera igual.
 */
class TestFileCipher : FileCipher {
    var failDecrypt = false

    override fun encrypt(plain: ByteArray): ByteArray = MAGIC + plain.map { (it.toInt() xor KEY).toByte() }

    override fun decrypt(stored: ByteArray): ByteArray {
        check(!failDecrypt) { "Llave perdida (prueba)" }
        if (!stored.startsWithMagic()) return stored
        return stored.copyOfRange(MAGIC.size, stored.size).map { (it.toInt() xor KEY).toByte() }.toByteArray()
    }

    private companion object {
        const val KEY = 0x5A
    }
}
