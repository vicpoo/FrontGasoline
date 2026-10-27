package org.vicpoo.gasolineproject.core.common

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
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
     * Fecha y hora LOCAL del teléfono CON su zona horaria: "2026-10-19T12:30:15-06:00".
     * La API (System.Text.Json / model binding) la convierte a la hora local del servidor, así
     * no importa si el teléfono y la PC del servidor tienen distinta zona horaria (antes se mandaba
     * sin zona y, si no coincidían, la API rechazaba puntos y cargas "fuera del periodo del viaje").
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
        val zone = timeZone()
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val local: LocalDateTime = instant.toLocalDateTime(zone)
        // LocalDate.toString() ya es ISO "2026-10-19"; UtcOffset.toString() es "-06:00" (o "Z")
        return buildString {
            append(local.date.toString()).append('T')
            append(local.hour.pad()).append(':')
            append(local.minute.pad()).append(':')
            append(local.second.pad())
            append(zone.offsetAt(instant).toString())
        }
    }

    private fun Int.pad() = toString().padStart(2, '0')
}
