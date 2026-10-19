package org.vicpoo.gasolineproject.core.common

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Formato de fechas de la API (DateTime de .NET en hora local del servidor, ej. "2026-09-24T10:32:15").
 * Sin dependencias extra: se lee el texto directamente.
 */
object AppDates {

    private val ISO = Regex("""^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})""")

    private val MONTHS = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
    )

    private data class Parts(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int)

    private fun parse(iso: String?): Parts? {
        val match = iso?.let { ISO.find(it.trim()) } ?: return null
        val (y, mo, d, h, mi) = match.destructured
        return Parts(y.toInt(), mo.toInt(), d.toInt(), h.toInt(), mi.toInt())
    }

    /** "24 de Septiembre del 2026" */
    fun longDate(iso: String?): String {
        val p = parse(iso) ?: return ""
        return "${p.day} de ${MONTHS[p.month - 1]} del ${p.year}"
    }

    /** "24/09/2026" */
    fun shortDate(iso: String?): String {
        val p = parse(iso) ?: return ""
        return "${p.day.pad()}/${p.month.pad()}/${p.year}"
    }

    /** "10:32 am" */
    fun time12(iso: String?): String {
        val p = parse(iso) ?: return ""
        val suffix = if (p.hour < 12) "am" else "pm"
        val hour12 = when (val h = p.hour % 12) {
            0 -> 12
            else -> h
        }
        return "$hour12:${p.minute.pad()} $suffix"
    }

    /** Minutos entre dos fechas de la API (null si falta alguna). */
    @OptIn(ExperimentalTime::class)
    fun minutesBetween(startIso: String?, endIso: String?): Long? {
        val start = toInstant(startIso) ?: return null
        val end = toInstant(endIso) ?: return null
        return (end - start).inWholeMinutes.coerceAtLeast(0)
    }

    /** 75 -> "1 h 15 min", 60 -> "1 h", 40 -> "40 min" */
    fun formatDuration(minutes: Long?): String {
        if (minutes == null) return "—"
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0L -> "$m min"
            m == 0L -> "$h h"
            else -> "$h h $m min"
        }
    }

    /** Para ordenar de más reciente a más antiguo. */
    fun sortKey(iso: String?): String = iso.orEmpty()

    @OptIn(ExperimentalTime::class)
    private fun toInstant(iso: String?): Instant? {
        val raw = iso?.trim() ?: return null
        // Sin zona horaria: se trata como UTC solo para poder restar (ambas fechas están en la misma zona)
        val normalized = if (raw.endsWith("Z") || raw.contains('+')) raw else "${raw}Z"
        return runCatching { Instant.parse(normalized) }.getOrNull()
    }

    private fun Int.pad() = toString().padStart(2, '0')
}
