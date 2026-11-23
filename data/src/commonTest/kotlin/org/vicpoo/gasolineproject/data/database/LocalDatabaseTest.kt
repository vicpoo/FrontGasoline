package org.vicpoo.gasolineproject.data.database

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.testing.InMemorySqlDriverFactory
import org.vicpoo.gasolineproject.testing.testLocalDatabase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class LocalDatabaseTest {

    private val factory = InMemorySqlDriverFactory()
    private val database = testLocalDatabase(factory)

    @AfterTest
    fun tearDown() = factory.closeAll()

    @Test
    fun noAbreLaBaseHastaQueAlguienLaPide() = runTest {
        assertEquals(0, factory.opened)

        database.get()

        assertEquals(1, factory.opened)
    }

    @Test
    fun aunqueLaPidanVariosALaVez_seAbreUnaSolaVez() = runTest {
        val all = List(5) { async { database.get() } }.awaitAll()

        assertEquals(1, factory.opened)
        all.forEach { assertSame(all.first(), it) }
    }

    @Test
    fun elEsquemaQuedaCreado_yGuardaValores() = runTest {
        val queries = database.get().appMetaQueries

        assertNull(queries.valueOf("json_migrado").executeAsOneOrNull())
        queries.put("json_migrado", "1")
        queries.put("json_migrado", "2")

        assertEquals("2", queries.valueOf("json_migrado").executeAsOne())
        queries.remove("json_migrado")
        assertNull(queries.valueOf("json_migrado").executeAsOneOrNull())
    }
}
