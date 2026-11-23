package org.vicpoo.gasolineproject.platform.database

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.vicpoo.gasolineproject.domain.common.AppLogLevel
import org.vicpoo.gasolineproject.domain.common.AppLogger
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * SQLCipher real en un celular o emulador: ./gradlew :platform:connectedAndroidDeviceTest
 * Usa su propia base, archivo de llave y alias del Keystore para no tocar los de la app.
 */
@RunWith(AndroidJUnit4::class)
class AndroidSqlDriverFactoryTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val keyFile = File(context.noBackupFilesDir, "test_local_db.key")
    private val passphrases = DatabasePassphraseStore(keyFile, keyAlias = "gasoline_local_db_test")
    private val logger = RecordingLogger()
    private val factory = AndroidSqlDriverFactory(context, logger, passphrases)
    private val drivers = mutableListOf<SqlDriver>()

    @After
    fun tearDown() {
        drivers.forEach { it.close() }
        context.deleteDatabase(DB_NAME)
        passphrases.delete()
    }

    @Test
    fun creaLaBase_yAlReabrirLeeLoQueSeGuardo() {
        open().insert("hola")
        closeLast()

        assertEquals(listOf("hola"), open().values())
        assertTrue(logger.errors.isEmpty())
    }

    @Test
    fun elArchivoEstaCifrado_noEsUnSqliteNormal() {
        open().insert("secreto")
        closeLast()

        val bytes = context.getDatabasePath(DB_NAME).readBytes()
        val header = bytes.copyOfRange(0, SQLITE_HEADER.size)
        assertFalse(header.contentEquals(SQLITE_HEADER), "La base quedó sin cifrar")
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("secreto"))
    }

    @Test
    fun laLlaveSeGuardaCifrada_yNoEsLaMismaEnDosInstalaciones() {
        open()
        val first = passphrases.read()!!
        assertEquals(DatabasePassphraseStore.PASSPHRASE_SIZE, first.size)
        assertFalse(keyFile.readBytes().toList().windowed(first.size).any { it == first.toList() })

        tearDown(); drivers.clear()
        open()
        assertNotEquals(first.toList(), passphrases.read()!!.toList())
    }

    @Test
    fun siSePierdeLaLlave_borraLaBase_laCreaVacia_yDejaUnError() {
        open().insert("viejo")
        closeLast()
        passphrases.delete() // como si el Keystore hubiera perdido la llave

        assertEquals(emptyList<String>(), open().values())
        assertEquals(1, logger.errors.size)
    }

    @Test
    fun siElArchivoDeLlaveSeDana_borraLaBase_yLaCreaVacia() {
        open().insert("viejo")
        closeLast()
        keyFile.writeBytes(ByteArray(40) { 7 })

        assertEquals(emptyList<String>(), open().values())
        assertEquals(1, logger.errors.size)
    }

    @Test
    fun llaveCruda_tieneElFormatoDeSqlCipher() {
        val key = AndroidSqlDriverFactory.rawKey(ByteArray(32) { it.toByte() }).decodeToString()
        assertEquals("x'000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f'", key)
    }

    private fun open(): SqlDriver = factory.create(DB_NAME, TestSchema).also { drivers += it }

    // removeAt y no removeLast: List.removeLast de Java 21 truena en Android < 15
    private fun closeLast() = drivers.removeAt(drivers.lastIndex).close()

    private fun SqlDriver.insert(value: String) {
        execute(null, "INSERT INTO nota(texto) VALUES (?)", 1) { bindString(0, value) }
    }

    private fun SqlDriver.values(): List<String> = executeQuery(
        identifier = null,
        sql = "SELECT texto FROM nota",
        mapper = { cursor ->
            val result = mutableListOf<String>()
            while (cursor.next().value) result += cursor.getString(0)!!
            QueryResult.Value(result)
        },
        parameters = 0,
    ).value

    private object TestSchema : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 1
        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.execute(null, "CREATE TABLE nota (texto TEXT NOT NULL)", 0)
            return QueryResult.Unit
        }
        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion,
        ): QueryResult.Value<Unit> = QueryResult.Unit
    }

    private class RecordingLogger : AppLogger {
        val errors = mutableListOf<String>()
        override fun isEnabled(level: AppLogLevel) = true
        override fun log(level: AppLogLevel, tag: String, message: String, throwable: Throwable?) {
            if (level == AppLogLevel.ERROR) errors += message
        }
    }

    private companion object {
        const val DB_NAME = "test_gasoline.db"
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
