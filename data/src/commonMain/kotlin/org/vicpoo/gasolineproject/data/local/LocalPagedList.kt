package org.vicpoo.gasolineproject.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.vicpoo.gasolineproject.data.database.LocalDatabase
import org.vicpoo.gasolineproject.data.network.LIST_PAGE_SIZE
import org.vicpoo.gasolineproject.database.AppDatabase
import org.vicpoo.gasolineproject.database.tables.ListItemEntity

/**
 * Lista larga que llega por páginas de la API (historial de viajes, solicitudes de reserva), guardada
 * fila por fila en ListItemEntity y recorrida por llave con [KeySetPaginationManager] (punto 1.6).
 *
 * Dos paginaciones distintas, que se combinan:
 * - En el teléfono: lo que se ve crece de [pageSize] en [pageSize] por llave ([showNextLocalPage]).
 *   Al abrir la app se ve la primera página aunque haya más guardadas.
 * - Con la API: [ListState] dice cuántas páginas se pidieron y si el servidor tiene más; el
 *   repositorio pide la siguiente solo cuando ya se ve todo lo guardado.
 */
class LocalPagedList<T : Any>(
    private val database: LocalDatabase,
    /** Nombre de la lista en la tabla ("trips", "my_reservations", ...). */
    private val name: String,
    private val serializer: KSerializer<T>,
    private val json: Json,
    private val idOf: (T) -> String,
    /** Llave de orden: la lista va de la más grande (más reciente) a la más chica. */
    private val sortKeyOf: (T) -> String,
    pageSize: Int = LIST_PAGE_SIZE,
) {
    /** Hasta dónde se pidió la lista a la API. */
    data class ListState(val loadedPages: Int = 0, val hasMore: Boolean = false)

    val pager = KeySetPaginationManager(pageSize)

    /** Lo que se ve, de la más reciente a la más vieja. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<List<T>> = pager.lastVisible.flatMapLatest { key ->
        flow {
            val db = database.get()
            val query = if (key == null) {
                db.listItemQueries.firstPage(name, pager.pageSize.toLong())
            } else {
                db.listItemQueries.upToKey(name, key.sortKey, key.itemId)
            }
            emitAll(query.asFlow().mapToList(Dispatchers.Default))
        }
    }.map { rows -> rows.mapNotNull { decode(it.json) } }

    fun observeById(id: String): Flow<T?> = flow {
        val db = database.get()
        emitAll(db.listItemQueries.byId(name, id).asFlow().mapToOneOrNull(Dispatchers.Default))
    }.map { it?.let(::decode) }

    fun observeState(): Flow<ListState> = flow {
        val db = database.get()
        emitAll(db.listStateQueries.get(name).asFlow().mapToOneOrNull(Dispatchers.Default))
    }.map { row -> row?.let { ListState(it.loaded_pages.toInt(), it.has_more != 0L) } ?: ListState() }

    /** Hay más para mostrar: guardadas que todavía no se ven, o más en la API. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeHasMore(): Flow<Boolean> {
        val hiddenLocally = pager.lastVisible.flatMapLatest { key ->
            flow {
                val db = database.get()
                val count = if (key == null) {
                    db.listItemQueries.countAll(name).asFlow().mapToOne(Dispatchers.Default).map { it > pager.pageSize }
                } else {
                    db.listItemQueries.countAfter(name, key.sortKey, key.itemId).asFlow().mapToOne(Dispatchers.Default).map { it > 0 }
                }
                emitAll(count)
            }
        }
        return combine(hiddenLocally, observeState()) { local, state -> local || state.hasMore }.distinctUntilChanged()
    }

    suspend fun state(): ListState = database.use {
        listStateQueries.get(name).executeAsOneOrNull()?.let { ListState(it.loaded_pages.toInt(), it.has_more != 0L) } ?: ListState()
    }

    /** Todas las guardadas (para conservar datos al recargar). */
    suspend fun all(): List<T> = database.use { listItemQueries.allForList(name).executeAsList() }.mapNotNull(::decode)

    suspend fun get(id: String): T? = database.use { listItemQueries.byId(name, id).executeAsOneOrNull() }?.let(::decode)

    /** Recarga: la lista queda exactamente como la mandó la API. Lo que se ve no se recorta. */
    suspend fun replaceAll(items: List<T>, state: ListState) = database.use {
        transaction {
            listItemQueries.clearList(name)
            items.forEach { insert(it) }
            putState(state)
        }
    }

    /** Página siguiente de la API: se agregan (o actualizan) sus filas. */
    suspend fun append(items: List<T>, state: ListState) = database.use {
        transaction {
            items.forEach { insert(it) }
            putState(state)
        }
    }

    /** Actualiza filas que ya están (ej. cargas y ruta de un viaje). */
    suspend fun upsert(items: List<T>) = database.use {
        transaction { items.forEach { insert(it) } }
    }

    suspend fun setState(state: ListState) = database.use { putState(state) }

    /**
     * Muestra la siguiente página de lo GUARDADO, por llave. Regresa false si ya se ve todo lo guardado
     * (entonces el repositorio pide la página siguiente a la API).
     */
    suspend fun showNextLocalPage(): Boolean {
        val next = database.use {
            val last = pager.lastVisible.value
                ?: listItemQueries.firstPage(name, pager.pageSize.toLong()).executeAsList().lastOrNull()?.key()
                ?: return@use emptyList()
            listItemQueries.pageAfter(name, last.sortKey, last.itemId, pager.pageSize.toLong()).executeAsList()
        }
        val lastOfPage = next.lastOrNull() ?: return false
        pager.showUpTo(lastOfPage.key())
        return true
    }

    /** Al cerrar sesión. */
    suspend fun clear() {
        database.use {
            transaction {
                listItemQueries.clearList(name)
                listStateQueries.remove(name)
            }
        }
        pager.reset()
    }

    private fun AppDatabase.insert(item: T) {
        listItemQueries.upsert(name, idOf(item), sortKeyOf(item), json.encodeToString(serializer, item))
    }

    private fun AppDatabase.putState(state: ListState) {
        listStateQueries.put(name, state.loadedPages.toLong(), if (state.hasMore) 1L else 0L)
    }

    private fun decode(text: String): T? = runCatching { json.decodeFromString(serializer, text) }.getOrNull()

    private fun ListItemEntity.key() = PageKey(sort_key, item_id)
}
