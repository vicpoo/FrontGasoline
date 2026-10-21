package org.vicpoo.gasolineproject.core.common

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Reloj de la app. Se inyecta (en vez de llamar a Clock.System directo) para poder probar
 * la lógica de viajes con un reloj falso.
 */
interface AppClock {
    /** Milisegundos desde 1970 (para medir duraciones). */
    fun nowMillis(): Long

    /**
     * Fecha y hora LOCAL sin zona, con segundos: "2026-10-19T12:30:15".
     * Así espera las fechas APIGasoline (DateTime en hora local del servidor).
     */
    fun nowLocalIso(): String = toLocalIso(nowMillis())

    fun toLocalIso(epochMillis: Long): String
}

@OptIn(ExperimentalTime::class)
class SystemAppClock(
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : AppClock {

    override fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    override fun toLocalIso(epochMillis: Long): String {
        val local: LocalDateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone())
        // LocalDate.toString() ya es ISO "2026-10-19"
        return buildString {
            append(local.date.toString()).append('T')
            append(local.hour.pad()).append(':')
            append(local.minute.pad()).append(':')
            append(local.second.pad())
        }
    }

    private fun Int.pad() = toString().padStart(2, '0')
}
