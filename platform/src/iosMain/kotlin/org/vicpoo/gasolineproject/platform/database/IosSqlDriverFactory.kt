package org.vicpoo.gasolineproject.platform.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.vicpoo.gasolineproject.domain.database.SqlDriverFactory

/**
 * iOS: SQLite normal, en la carpeta de la app (protegida por el cifrado de iOS mientras el teléfono
 * está bloqueado).
 *
 * TODO(iOS, punto 4.5): cifrar con SQLCipher y guardar la contraseña en el Keychain, igual que en
 * Android. iOS está fuera del alcance de la revisión.
 */
class IosSqlDriverFactory : SqlDriverFactory {
    override fun create(name: String, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver =
        NativeSqliteDriver(schema, name)
}
