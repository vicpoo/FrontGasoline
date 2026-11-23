package org.vicpoo.gasolineproject.testing

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import org.vicpoo.gasolineproject.data.database.AppDatabase
import org.vicpoo.gasolineproject.data.database.LocalDatabase
import org.vicpoo.gasolineproject.domain.database.SqlDriverFactory

/** Base SQLite en memoria con [schema] ya creado (JDBC en la JVM, driver nativo en iOS). */
expect fun inMemorySqlDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver

/** Para pruebas: cada [create] da una base nueva en memoria y cuenta cuántas veces se abrió. */
class InMemorySqlDriverFactory : SqlDriverFactory {
    var opened = 0
        private set
    val drivers = mutableListOf<SqlDriver>()

    override fun create(name: String, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
        opened++
        return inMemorySqlDriver(schema).also { drivers += it }
    }

    fun closeAll() = drivers.forEach { it.close() }
}

/** [LocalDatabase] en memoria para pruebas de repositorios. */
fun testLocalDatabase(factory: InMemorySqlDriverFactory = InMemorySqlDriverFactory()): LocalDatabase =
    LocalDatabase(driverFactory = factory)

/** [AppDatabase] en memoria lista para usar (sin pasar por [LocalDatabase]). */
fun testAppDatabase(): AppDatabase = AppDatabase(inMemorySqlDriver(AppDatabase.Schema))
