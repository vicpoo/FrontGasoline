package org.vicpoo.gasolineproject.domain.files

/**
 * Cifra las fotos que la app guarda mientras esperan subirse (recibos, odómetro, vehículos).
 * La implementa platform: en Android, AES-256-GCM con una llave del Android Keystore.
 *
 * Los archivos cifrados empiezan con [MAGIC]. Los guardados antes de esta versión no lo tienen:
 * [decrypt] los regresa tal cual, para no perder fotos pendientes al actualizar la app.
 */
interface FileCipher {
    fun encrypt(plain: ByteArray): ByteArray

    /** Descifra; si [stored] no está cifrado (archivo viejo) lo regresa igual. Lanza si no se puede descifrar. */
    fun decrypt(stored: ByteArray): ByteArray

    fun isEncrypted(stored: ByteArray): Boolean = stored.startsWithMagic()

    companion object {
        /** Encabezado de los archivos cifrados por la app ("GPF1"). */
        val MAGIC: ByteArray = byteArrayOf(0x47, 0x50, 0x46, 0x31)

        fun ByteArray.startsWithMagic(): Boolean =
            size >= MAGIC.size && MAGIC.indices.all { this[it] == MAGIC[it] }
    }
}

/**
 * Sin cifrado: lo usa iOS por ahora (TODO: Keychain + CryptoKit, iOS fuera del alcance de la revisión).
 * En iOS nunca se escriben archivos cifrados, así que leer es regresar los bytes tal cual.
 */
object PlainFileCipher : FileCipher {
    override fun encrypt(plain: ByteArray): ByteArray = plain
    override fun decrypt(stored: ByteArray): ByteArray = stored
    override fun isEncrypted(stored: ByteArray): Boolean = false
}
