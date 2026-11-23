package org.vicpoo.gasolineproject.platform.database

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.vicpoo.gasolineproject.domain.common.AppLogger
import org.vicpoo.gasolineproject.domain.common.e
import org.vicpoo.gasolineproject.domain.database.SqlDriverFactory
import java.io.File

/**
 * Android: la base local se cifra completa con SQLCipher (AES-256). La contraseña la guarda
 * [DatabasePassphraseStore], cifrada con una llave del Android Keystore.
 *
 * Recuperación: si la contraseña ya no se puede leer (la llave del Keystore se perdió) o la base no
 * abre con ella, se borra la base, se crea vacía con una contraseña nueva y se escribe un ERROR en la
 * bitácora. Los datos se vuelven a descargar del servidor; lo que no se había enviado se pierde (caso
 * muy raro: el Keystore no se respalda y la base tampoco, ver data_extraction_rules.xml).
 */
class AndroidSqlDriverFactory internal constructor(
    private val context: Context,
    private val logger: AppLogger,
    private val passphrases: DatabasePassphraseStore,
) : SqlDriverFactory {

    constructor(context: Context, logger: AppLogger) : this(
        context = context,
        logger = logger,
        passphrases = DatabasePassphraseStore(File(context.noBackupFilesDir, KEY_FILE_NAME)),
    )

    override fun create(name: String, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
        loadSqlCipher()
        val databaseExists = context.getDatabasePath(name).exists()

        var passphrase = passphrases.read()
        if (passphrase == null) {
            if (databaseExists || passphrases.exists()) {
                logger.e(TAG, "No se pudo leer la llave de la base local; se borra y se vuelve a descargar")
                context.deleteDatabase(name)
            }
            passphrases.delete()
            passphrase = passphrases.createNew()
        }

        return try {
            open(name, schema, passphrase)
        } catch (error: Exception) {
            logger.e(TAG, "La base local no abre con su llave; se borra y se crea de nuevo", error)
            context.deleteDatabase(name)
            open(name, schema, passphrase)
        }
    }

    /** Abre de verdad (AndroidSqliteDriver abre hasta la primera consulta) para detectar aquí una llave incorrecta. */
    private fun open(name: String, schema: SqlSchema<QueryResult.Value<Unit>>, passphrase: ByteArray): SqlDriver {
        val driver = AndroidSqliteDriver(
            schema = schema,
            context = context,
            name = name,
            factory = SupportOpenHelperFactory(rawKey(passphrase)),
        )
        try {
            driver.executeQuery(null, "SELECT count(*) FROM sqlite_master", { QueryResult.Value(it.next().value) }, 0)
            return driver
        } catch (error: Exception) {
            driver.close()
            throw error
        }
    }

    internal companion object {
        const val KEY_FILE_NAME = "local_db.key"
        private const val TAG = "BaseLocal"

        @Volatile
        private var sqlCipherLoaded = false

        private fun loadSqlCipher() {
            if (sqlCipherLoaded) return
            synchronized(this) {
                if (!sqlCipherLoaded) {
                    System.loadLibrary("sqlcipher")
                    sqlCipherLoaded = true
                }
            }
        }

        /**
         * Llave "cruda" para SQLCipher: x'<64 hex>'. Con este formato SQLCipher usa los 32 bytes tal
         * cual y se salta la derivación PBKDF2 (≈ 256 000 vueltas): la contraseña ya es aleatoria,
         * así que derivarla no agrega seguridad y abrir la base tardaría mucho más.
         */
        fun rawKey(passphrase: ByteArray): ByteArray =
            ("x'" + passphrase.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') } + "'").encodeToByteArray()
    }
}
