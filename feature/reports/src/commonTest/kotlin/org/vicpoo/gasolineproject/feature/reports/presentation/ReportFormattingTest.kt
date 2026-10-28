package org.vicpoo.gasolineproject.feature.reports.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ReportFormattingTest {

    @Test
    fun numeros_conComasYSinDecimalesSobrantes() {
        assertEquals("12,345.68", ReportFormatting.number(12345.678))
        assertEquals("1,000", ReportFormatting.number(1000.0))
        assertEquals("0.5", ReportFormatting.number(0.5))
        assertEquals("999", ReportFormatting.number(999.0))
    }

    @Test
    fun dinero_siempreConCentavos() {
        assertEquals("$1,234.50", ReportFormatting.money(1234.5))
        assertEquals("$0.00", ReportFormatting.money(0.0))
        assertEquals("$1,000,000.00", ReportFormatting.money(1_000_000.0))
    }

    @Test
    fun enteros_conComas() {
        assertEquals("1,234", ReportFormatting.integer(1234))
        assertEquals("7", ReportFormatting.integer(7))
    }
}
