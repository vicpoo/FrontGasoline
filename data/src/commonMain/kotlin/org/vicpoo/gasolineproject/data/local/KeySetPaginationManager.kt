package org.vicpoo.gasolineproject.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Llave de una fila en una lista ordenada de la más reciente a la más vieja. */
data class PageKey(val sortKey: String, val itemId: String)

/**
 * Paginación por llave (keyset) de una lista guardada en la base (punto 1.6 del code review).
 *
 * En vez de "dame las filas 30 a 40" (OFFSET, que obliga a SQLite a recorrer las 30 primeras y se
 * desordena si llega una nueva arriba), recuerda la llave de la ÚLTIMA fila que se ve:
 * - Lo que se ve: las filas desde la más reciente hasta [lastVisible] (incluida).
 * - La página siguiente: las [pageSize] filas con llave menor que [lastVisible].
 * Sin [lastVisible] (al abrir) se ve solo la primera página.
 *
 * Solo guarda la llave; las consultas las hace [LocalPagedList] (ListItem.sq).
 */
class KeySetPaginationManager(val pageSize: Int) {

    private val lastVisibleKey = MutableStateFlow<PageKey?>(null)

    /** Llave de la última fila que se ve; null = solo la primera página. */
    val lastVisible: StateFlow<PageKey?> = lastVisibleKey.asStateFlow()

    /** Se mostró una página más: ahora la última fila visible es [key]. */
    fun showUpTo(key: PageKey) {
        lastVisibleKey.value = key
    }

    /** Volver a la primera página (al cerrar sesión). */
    fun reset() {
        lastVisibleKey.value = null
    }
}
