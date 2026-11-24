package org.vicpoo.gasolineproject.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.data.local.LocalPagedList.ListState
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.pagedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Listas largas guardadas fila por fila y recorridas por llave (punto 1.6), con páginas de 3. */
class LocalPagedListTest {

    @Serializable
    data class Item(val id: String, val date: String)

    private val storage = TestStorage()

    private fun newList() = storage.pagedList("prueba", Item.serializer(), { it.id }, { it.date }, pageSize = 3)

    private val list = newList()

    /** 8 elementos: el 8 es el más reciente. */
    private val items = (1..8).map { Item("i$it", "2026-11-0${it}T10:00:00") }

    private suspend fun visibleIds(l: LocalPagedList<Item> = list) = l.observe().first().map { it.id }

    @Test
    fun alAbrir_seVeLaPrimeraPagina_yLasSiguientesSePidenPorLlave() = runTest {
        list.replaceAll(items, ListState(loadedPages = 1, hasMore = false))

        assertEquals(listOf("i8", "i7", "i6"), visibleIds())
        assertTrue(list.observeHasMore().first(), "Hay guardados que todavía no se ven")

        assertTrue(list.showNextLocalPage())
        assertEquals(listOf("i8", "i7", "i6", "i5", "i4", "i3"), visibleIds())

        assertTrue(list.showNextLocalPage())
        assertEquals(items.reversed().map { it.id }, visibleIds())
        assertFalse(list.observeHasMore().first())

        assertFalse(list.showNextLocalPage(), "Ya se ve todo lo guardado: toca pedir a la API")
    }

    @Test
    fun siLaApiTieneMas_hayMasAunqueSeVeaTodoLoGuardado() = runTest {
        list.replaceAll(items.takeLast(2), ListState(loadedPages = 1, hasMore = true))

        assertEquals(listOf("i8", "i7"), visibleIds())
        assertTrue(list.observeHasMore().first())
        assertFalse(list.showNextLocalPage())
    }

    @Test
    fun mismaFecha_seDesempataPorId_sinRepetirNiSaltarse() = runTest {
        val sameDay = listOf("a", "b", "c", "d", "e").map { Item(it, "2026-11-20T10:00:00") }
        list.replaceAll(sameDay, ListState())

        list.showNextLocalPage()

        assertEquals(listOf("e", "d", "c", "b", "a"), visibleIds())
    }

    @Test
    fun alRecargar_noSeRecortaLoQueYaSeVeia_yLoNuevoApareceArriba() = runTest {
        list.replaceAll(items.take(6), ListState(loadedPages = 1))
        list.showNextLocalPage() // se ven los 6

        list.replaceAll(items.take(7), ListState(loadedPages = 1)) // llegó el i7

        assertEquals(listOf("i7", "i6", "i5", "i4", "i3", "i2", "i1"), visibleIds())
    }

    @Test
    fun agregarPaginaDeLaApi_yMostrarla() = runTest {
        list.replaceAll(items.takeLast(3), ListState(loadedPages = 1, hasMore = true))

        list.append(items.take(3), ListState(loadedPages = 2, hasMore = false))
        list.showNextLocalPage()

        assertEquals(listOf("i8", "i7", "i6", "i3", "i2", "i1"), visibleIds())
        assertEquals(ListState(loadedPages = 2, hasMore = false), list.state())
    }

    @Test
    fun alVolverAAbrirLaApp_seVeSoloLaPrimeraPagina_peroTodoSigueGuardado() = runTest {
        list.replaceAll(items, ListState(loadedPages = 1))
        list.showNextLocalPage()

        val reopened = newList()

        assertEquals(listOf("i8", "i7", "i6"), visibleIds(reopened))
        assertEquals(8, reopened.all().size)
        assertEquals("i2", reopened.get("i2")?.id)
    }

    @Test
    fun cerrarSesion_borraLaLista_suEstado_yVuelveALaPrimeraPagina() = runTest {
        list.replaceAll(items, ListState(loadedPages = 1, hasMore = true))
        list.showNextLocalPage()

        list.clear()

        assertTrue(visibleIds().isEmpty())
        assertEquals(ListState(), list.state())
        list.replaceAll(items, ListState())
        assertEquals(listOf("i8", "i7", "i6"), visibleIds())
    }
}
