package org.vicpoo.gasolineproject.domain.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * Abre la base local de la app. La implementa platform (igual que el GPS o la cámara) y la usa data,
 * que es dueña del esquema (AppDatabase). Así platform no conoce data.
 *
 * - Android: SQLCipher; la contraseña de la base se guarda cifrada con una llave del Android Keystore.
 * - iOS: SQLite sin cifrar por ahora (TODO; iOS está fuera del alcance de la revisión).
 *
 * Si la base existe pero ya no se puede abrir (se perdió la llave del Keystore o el archivo está dañado),
 * la implementación la borra, la crea vacía y deja un ERROR en la bitácora: los datos se vuelven a
 * descargar del servidor. Se llama una sola vez y fuera del hilo principal (abrir la base cuesta).
 */
interface SqlDriverFactory {
    fun create(name: String, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver
}
