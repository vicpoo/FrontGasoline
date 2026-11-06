package org.vicpoo.gasolineproject.domain.reports

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
            // Las fechas las eligió el usuario: ver rangeOf(filters, ...)
            ReportPeriod.CUSTOM -> null
        }
    }

    /** Rango de los filtros: el elegido en el calendario (CUSTOM) o el del periodo. */
    fun rangeOf(filters: ReportFilters, todayIso: String): DateRange? {
        if (filters.period == ReportPeriod.CUSTOM) {
            val from = filters.customFrom ?: return null
            val to = filters.customTo ?: from
            // Por si llegan al revés
            return if (from <= to) DateRange(from, to) else DateRange(to, from)
        }
        return rangeOf(filters.period, todayIso)
    }

    // ---------- Fechas del calendario (DateRangePicker usa milisegundos UTC a medianoche) ----------

    private const val MILLIS_PER_DAY = 86_400_000L

    /** Milisegundos UTC (medianoche) -> "yyyy-MM-dd". */
    fun dateFromUtcMillis(utcMillis: Long): String {
        val days = utcMillis.floorDiv(MILLIS_PER_DAY)
        val (y, m, d) = civilFromDays(days)
        return date(y, m, d)
    }

    /** "yyyy-MM-dd" -> milisegundos UTC a medianoche (null si no es una fecha). */
    fun utcMillisOf(iso: String?): Long? {
        val (y, m, d) = iso?.let(::parseDate) ?: return null
        return daysFromCivil(y, m, d) * MILLIS_PER_DAY
    }

    /** Días desde 1970-01-01 (algoritmo de Howard Hinnant, calendario gregoriano). */
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = y.floorDiv(400L)
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    private fun civilFromDays(daysSinceEpoch: Long): Triple<Int, Int, Int> {
        val z = daysSinceEpoch + 719_468
        val era = z.floorDiv(146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
        return Triple(year, month, day)
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
