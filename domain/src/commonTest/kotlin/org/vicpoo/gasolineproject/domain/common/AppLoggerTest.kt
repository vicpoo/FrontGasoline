package org.vicpoo.gasolineproject.domain.common

import org.vicpoo.gasolineproject.testing.FakeAppLogger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Niveles de la bitácora y el acceso global (AppLog) para el código que no usa Koin. */
class AppLoggerTest {

    @AfterTest
    fun tearDown() = AppLog.reset()

    @Test
    fun enRelease_soloAvisosYErrores_enDebugTodo() {
        assertEquals(AppLogLevel.WARN, minLogLevel(isDebug = false))
        assertEquals(AppLogLevel.DEBUG, minLogLevel(isDebug = true))
    }

    @Test
    fun antesDeInstalar_noEscribeNada() {
        AppLog.e("Prueba", "No debe fallar")

        assertFalse(AppLog.logger.isEnabled(AppLogLevel.ERROR))
    }

    @Test
    fun despuesDeInstalar_usaElLoggerDeLaPlataforma() {
        val logger = FakeAppLogger()
        AppLog.install(logger)

        AppLog.w("API", "HTTP 500 GET /api/viajes")
        AppLog.e("AppScope", "Falló", IllegalStateException("x"))

        assertEquals(listOf(AppLogLevel.WARN, AppLogLevel.ERROR), logger.entries.map { it.level })
        assertEquals("API", logger.entries.first().tag)
    }
}
