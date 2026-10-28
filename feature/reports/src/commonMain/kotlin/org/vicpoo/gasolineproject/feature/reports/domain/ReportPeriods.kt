package org.vicpoo.gasolineproject.feature.reports.domain

/** Rango de fechas "yyyy-MM-dd" (ambos días incluidos). */
data class DateRange(val from: String, val to: String)

/**
 * Convierte un [ReportPeriod] en fechas a partir de "hoy" (AppClock.nowLocalIso()).
 * Lógica pura (sin reloj ni librerías) para poder probarla.
 */
object ReportPeriods {

    /** null = sin límite (todo el historial). */
    fun rangeOf(period: ReportPeriod, todayIso: String): DateRange? {
        val (year, month, day) = parseDate(todayIso) ?: return null
        val today = date(year, month, day)
        return when (period) {
            ReportPeriod.THIS_MONTH -> DateRange(date(year, month, 1), today)
            ReportPeriod.LAST_MONTH -> {
                val (y, m) = minusMonths(year, month, 1)
                DateRange(date(y, m, 1), date(y, m, daysInMonth(y, m)))
            }
            // El mes actual y los dos anteriores completos
            ReportPeriod.LAST_3_MONTHS -> {
                val (y, m) = minusMonths(year, month, 2)
                DateRange(date(y, m, 1), today)
            }
            ReportPeriod.THIS_YEAR -> DateRange(date(year, 1, 1), today)
            ReportPeriod.ALL_TIME -> null
        }
    }

    /** "2026-10-28..." -> (2026, 10, 28) */
    internal fun parseDate(iso: String): Triple<Int, Int, Int>? {
        val match = DATE.find(iso.trim()) ?: return null
        val (y, m, d) = match.destructured
        return Triple(y.toInt(), m.toInt(), d.toInt())
    }

    internal fun daysInMonth(year: Int, month: Int): Int = when (month) {
        2 -> if (isLeap(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    private fun isLeap(year: Int) = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    private fun minusMonths(year: Int, month: Int, months: Int): Pair<Int, Int> {
        val index = year * 12 + (month - 1) - months
        return index / 12 to index % 12 + 1
    }

    private fun date(year: Int, month: Int, day: Int) =
        "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    private val DATE = Regex("""^(\d{4})-(\d{2})-(\d{2})""")
}
