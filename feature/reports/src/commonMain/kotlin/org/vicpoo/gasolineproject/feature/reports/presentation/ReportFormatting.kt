package org.vicpoo.gasolineproject.feature.reports.presentation

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Números del reporte como en México: "12,345.6" y "$1,234.50".
 * Lógica pura (sin Locale de plataforma) para que se vea igual en Android e iOS.
 */
internal object ReportFormatting {

    /** 12345.678 -> "12,345.68"; 3.0 -> "3" (sin decimales sobrantes). */
    fun number(value: Double, decimals: Int = 2): String {
        val factor = pow10(decimals)
        val scaled = (abs(value) * factor).roundToLong()
        val whole = scaled / factor
        val fraction = (scaled % factor).toString().padStart(decimals, '0').trimEnd('0')
        val sign = if (value < 0 && scaled != 0L) "-" else ""
        return sign + groupThousands(whole) + if (fraction.isEmpty()) "" else ".$fraction"
    }

    /** 1234.5 -> "$1,234.50" (siempre con centavos). */
    fun money(value: Double): String {
        val cents = (abs(value) * 100).roundToLong()
        val sign = if (value < 0 && cents != 0L) "-" else ""
        return sign + "\$" + groupThousands(cents / 100) + "." + (cents % 100).toString().padStart(2, '0')
    }

    fun integer(value: Int): String = (if (value < 0) "-" else "") + groupThousands(abs(value.toLong()))

    private fun groupThousands(value: Long): String =
        value.toString().reversed().chunked(3).joinToString(",").reversed()

    private fun pow10(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 10 }
        return result
    }
}
