package org.vicpoo.gasolineproject.feature.triprecording.data.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Reconocer el viaje del teléfono en el 409 VIAJE_EN_CURSO (operador + hora de inicio ±2 min). */
class ServerTripMatchTest {

    @Test
    fun mismaHoraConYSinZona_daLoMismo() {
        assertEquals(
            ServerTripMatch.wallClockSeconds("2026-11-04T17:40:00-06:00"),
            ServerTripMatch.wallClockSeconds("2026-11-04T17:40:00"),
        )
        assertEquals(
            ServerTripMatch.wallClockSeconds("2026-11-04T17:40:00"),
            ServerTripMatch.wallClockSeconds("2026-11-04T17:40:00.1234567"),
        )
    }

    @Test
    fun cambioDeDia_seCalculaBien() {
        val antes = ServerTripMatch.wallClockSeconds("2026-02-28T23:59:30")!!
        val despues = ServerTripMatch.wallClockSeconds("2026-03-01T00:00:30")!!
        assertEquals(60L, despues - antes)
        assertEquals(0L, ServerTripMatch.wallClockSeconds("1970-01-01T00:00:00"))
    }

    @Test
    fun textoSinFormato_esNull() {
        assertNull(ServerTripMatch.wallClockSeconds("ayer"))
        assertNull(ServerTripMatch.wallClockSeconds("2026-13-01T00:00:00"))
    }

    @Test
    fun mismoOperadorDentroDeLaTolerancia_esElMismoViaje() {
        assertTrue(ServerTripMatch.isSameTrip("EMP-1", "2026-11-04T17:40:00-06:00", "emp-1", "2026-11-04T17:41:59"))
    }

    @Test
    fun fueraDeLaTolerancia_oOtroOperador_noEsElMismo() {
        assertFalse(ServerTripMatch.isSameTrip("emp-1", "2026-11-04T17:40:00-06:00", "emp-1", "2026-11-04T17:42:01"))
        assertFalse(ServerTripMatch.isSameTrip("emp-1", "2026-11-04T17:40:00-06:00", "emp-2", "2026-11-04T17:40:00"))
        assertFalse(ServerTripMatch.isSameTrip("emp-1", "2026-11-04T17:40:00-06:00", null, "2026-11-04T17:40:00"))
    }

    @Test
    fun soloSeMandanUuidsValidos() {
        assertEquals("3f2b8c1e-9d4a-4e6b-8f1a-2c7d5e9b0a11", "3f2b8c1e-9d4a-4e6b-8f1a-2c7d5e9b0a11".asUuidOrNull())
        assertNull("trip-1".asUuidOrNull())
        assertNull("00000000-0000-0000-0000-000000000000".asUuidOrNull())
    }
}
