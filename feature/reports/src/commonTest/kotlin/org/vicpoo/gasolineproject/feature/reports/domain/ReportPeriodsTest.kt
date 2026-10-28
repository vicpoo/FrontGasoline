package org.vicpoo.gasolineproject.feature.reports.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Rangos de fechas de cada periodo del reporte. */
class ReportPeriodsTest {

    private val today = "2026-10-28T12:30:15-06:00"

    @Test
    fun esteMes_vaDelDiaPrimeroAHoy() =
        assertEquals(DateRange("2026-10-01", "2026-10-28"), ReportPeriods.rangeOf(ReportPeriod.THIS_MONTH, today))

    @Test
    fun mesPasado_esElMesCompleto() =
        assertEquals(DateRange("2026-09-01", "2026-09-30"), ReportPeriods.rangeOf(ReportPeriod.LAST_MONTH, today))

    @Test
    fun mesPasado_enEnero_esDiciembreDelAnoAnterior() =
        assertEquals(
            DateRange("2025-12-01", "2025-12-31"),
            ReportPeriods.rangeOf(ReportPeriod.LAST_MONTH, "2026-01-15T08:00:00-06:00"),
        )

    @Test
    fun mesPasado_febreroBisiesto() =
        assertEquals(
            DateRange("2028-02-01", "2028-02-29"),
            ReportPeriods.rangeOf(ReportPeriod.LAST_MONTH, "2028-03-10T08:00:00Z"),
        )

    @Test
    fun ultimosTresMeses_incluyeElMesActual() {
        assertEquals(DateRange("2026-08-01", "2026-10-28"), ReportPeriods.rangeOf(ReportPeriod.LAST_3_MONTHS, today))
        assertEquals(
            DateRange("2025-12-01", "2026-02-05"),
            ReportPeriods.rangeOf(ReportPeriod.LAST_3_MONTHS, "2026-02-05T08:00:00Z"),
        )
    }

    @Test
    fun esteAno_vaDelPrimeroDeEnero() =
        assertEquals(DateRange("2026-01-01", "2026-10-28"), ReportPeriods.rangeOf(ReportPeriod.THIS_YEAR, today))

    @Test
    fun todo_noTieneLimites() = assertNull(ReportPeriods.rangeOf(ReportPeriod.ALL_TIME, today))
}
