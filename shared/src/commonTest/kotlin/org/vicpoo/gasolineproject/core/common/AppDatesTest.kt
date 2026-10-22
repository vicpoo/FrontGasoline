package org.vicpoo.gasolineproject.core.common

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppDatesTest {

    @Test
    fun formatsApiDates() {
        val iso = "2026-09-24T10:32:15"
        assertEquals("24 de Septiembre del 2026", AppDates.longDate(iso))
        assertEquals("24/09/2026", AppDates.shortDate(iso))
        assertEquals("10:32 am", AppDates.time12(iso))
        assertEquals("12:05 pm", AppDates.time12("2026-09-24T12:05:00"))
        assertEquals("12:00 am", AppDates.time12("2026-09-24T00:00:00"))
    }

    @Test
    fun invalidDates_returnEmptyText() {
        assertEquals("", AppDates.longDate(null))
        assertEquals("", AppDates.shortDate("hola"))
    }

    @Test
    fun minutesBetween_supportsDatesWithAndWithoutOffset() {
        assertEquals(75, AppDates.minutesBetween("2026-10-19T10:00:00", "2026-10-19T11:15:00"))
        // Misma hora real escrita en dos zonas distintas
        assertEquals(0, AppDates.minutesBetween("2026-10-19T10:00:00-06:00", "2026-10-19T16:00:00Z"))
        assertNull(AppDates.minutesBetween(null, "2026-10-19T11:15:00"))
    }

    @Test
    fun formatsDurations() {
        assertEquals("40 min", AppDates.formatDuration(40))
        assertEquals("1 h", AppDates.formatDuration(60))
        assertEquals("1 h 15 min", AppDates.formatDuration(75))
        assertEquals("—", AppDates.formatDuration(null))
    }

    @Test
    fun clock_sendsLocalTimeWithOffset() {
        // 2026-10-19T16:14:05Z en Ciudad de México (UTC-6, sin horario de verano)
        val clock = SystemAppClock(timeZone = { TimeZone.of("America/Mexico_City") })
        assertEquals("2026-10-19T10:14:05-06:00", clock.toLocalIso(1_792_426_445_000L))
    }
}
