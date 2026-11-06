package org.vicpoo.gasolineproject.core.session

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Tokens de la sesión. Se guardan SOLO en el almacenamiento seguro del sistema ([TokenStore]):
 * Keystore en Android, Keychain en iOS. Nunca en el DataStore en texto plano ni en [Session].
 *
 * - [accessToken]: JWT corto (15 min) que va en "Authorization: Bearer".
 * - [refreshToken]: sesión larga (30 días deslizantes); solo sirve para POST /api/auth/refresh.
 *   Cada renovación lo reemplaza por uno nuevo (rotación).
 * Las fechas vienen de la API en UTC (ISO-8601).
 */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val accessExpiresAtUtc: String? = null,
    /** null solo si la API todavía no manda refresh token (versión anterior): la sesión dura lo que el JWT. */
    val refreshToken: String? = null,
    val refreshExpiresAtUtc: String? = null,
    val sessionId: String? = null,
)

/** Lectura de las fechas de expiración de los tokens (UTC). */
@OptIn(ExperimentalTime::class)
object TokenExpiry {
    private val OFFSET_SUFFIX = Regex("""(Z|[+-]\d{2}:\d{2})$""")

    /** Milisegundos UTC de la fecha, o null si no se puede leer. Sin zona se asume UTC (así la manda la API). */
    fun epochMillis(isoUtc: String?): Long? {
        val raw = isoUtc?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val normalized = if (OFFSET_SUFFIX.containsMatchIn(raw)) raw else "${raw}Z"
        return runCatching { Instant.parse(normalized).toEpochMilliseconds() }.getOrNull()
    }

    /** true si ya venció o vence dentro de [marginMillis]. Si la fecha no se puede leer, se asume vigente. */
    fun expiresWithin(isoUtc: String?, nowMillis: Long, marginMillis: Long = 0): Boolean {
        val expiresAt = epochMillis(isoUtc) ?: return false
        return nowMillis + marginMillis >= expiresAt
    }
}

/**
 * true si la sesión ya no se puede usar: venció el refresh token (o, si la API no manda refresh token,
 * venció el JWT). Sin fechas se asume vigente y la API decide con 401.
 */
fun AuthTokens.isSessionExpired(nowMillis: Long): Boolean =
    if (refreshToken != null) {
        TokenExpiry.expiresWithin(refreshExpiresAtUtc, nowMillis)
    } else {
        TokenExpiry.expiresWithin(accessExpiresAtUtc, nowMillis)
    }
