package org.vicpoo.gasolineproject.data.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.vicpoo.gasolineproject.database.AppDatabase
import org.vicpoo.gasolineproject.domain.database.SqlDriverFactory

/**
 * La base local de la app (SQLDelight, archivo [FILE_NAME]). El esquema y el código generado
 * ([AppDatabase]) viven en el módulo :database.
 *
 * Se abre la primera vez que alguien la pide con [get], fuera del hilo principal: en Android abrir
 * SQLCipher (y, si hace falta, crear o recuperar la llave) tarda. Koin crea esta clase al armar los
 * repositorios, a veces en el hilo principal, por eso no se abre en el constructor.
 */
class LocalDatabase(
    private val driverFactory: SqlDriverFactory,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val mutex = Mutex()
    private var database: AppDatabase? = null

    suspend fun get(): AppDatabase = mutex.withLock {
        database ?: withContext(dispatcher) {
            AppDatabase(driverFactory.create(FILE_NAME, AppDatabase.Schema))
        }.also { database = it }
    }

    companion object {
        const val FILE_NAME = "gasoline.db"
    }
}
