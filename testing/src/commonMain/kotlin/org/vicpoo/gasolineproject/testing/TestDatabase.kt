package org.vicpoo.gasolineproject.testing

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import org.vicpoo.gasolineproject.data.database.LocalDatabase
import org.vicpoo.gasolineproject.database.AppDatabase
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

/** Lista paginada por llave en la base de pruebas (de la más reciente a la más vieja). */
fun <T : Any> TestStorage.pagedList(
    name: String,
    serializer: kotlinx.serialization.KSerializer<T>,
    idOf: (T) -> String,
    sortKeyOf: (T) -> String,
    pageSize: Int = org.vicpoo.gasolineproject.data.network.LIST_PAGE_SIZE,
) = org.vicpoo.gasolineproject.data.local.LocalPagedList(
    database = database,
    name = name,
    serializer = serializer,
    json = testJson,
    idOf = idOf,
    sortKeyOf = sortKeyOf,
    pageSize = pageSize,
)

/** Las dos listas de solicitudes y el badge, como las arma ReservationsDataModule. */
class TestReservationStorage(storage: TestStorage) {
    val mine = storage.pagedList("my_reservations", org.vicpoo.gasolineproject.data.reservations.local.ReservationRequestEntity.serializer(), { it.id }, { it.requestedAt.orEmpty() })
    val company = storage.pagedList("company_reservations", org.vicpoo.gasolineproject.data.reservations.local.ReservationRequestEntity.serializer(), { it.id }, { it.requestedAt.orEmpty() })
    val updates = org.vicpoo.gasolineproject.data.local.LocalDocumentCache(
        storage.database, storage.fileSystem, storage.directories, "my_reservations.json",
        org.vicpoo.gasolineproject.data.reservations.local.MyReservationsSnapshot.serializer(), testJson,
        org.vicpoo.gasolineproject.data.reservations.local.MyReservationsSnapshot(),
    )
}
