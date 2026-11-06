package org.vicpoo.gasolineproject.data.triprecording.sync

import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.httpCode
import org.vicpoo.gasolineproject.domain.common.serverMessage
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.data.network.safeApiCall
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.domain.sync.SyncRejection
import org.vicpoo.gasolineproject.domain.sync.stopsSync
import org.vicpoo.gasolineproject.data.triprecording.local.FuelEntity
import org.vicpoo.gasolineproject.data.triprecording.local.FuelSyncState
import org.vicpoo.gasolineproject.data.triprecording.local.PauseEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TrackPointEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.data.triprecording.remote.CargaFormData
import org.vicpoo.gasolineproject.data.triprecording.remote.FinalizarViajeRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.IniciarViajeRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.PausaSincronizadaDto
import org.vicpoo.gasolineproject.data.triprecording.remote.PausarViajeRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.ReanudarViajeRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.PuntoRutaRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.ReceiptUpload
import org.vicpoo.gasolineproject.data.triprecording.remote.SincronizarViajeRequestDto
import org.vicpoo.gasolineproject.data.triprecording.remote.TripRecordingApi
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import kotlin.math.round

/** Resultado de un paso de la sincronización de un viaje. */
internal sealed interface Step {
    data object Done : Step

    /** La API rechazó este registro: el error quedó guardado en él y se sigue con lo demás. */
    data object Rejected : Step

    /**
     * Error temporal del servidor (5xx, 408, 429...): no se marca como enviado y se reintenta en la
     * siguiente sincronización. Los pasos que dependen de él (ej. finalizar el viaje) esperan.
     */
    data class Deferred(val error: AppError) : Step

    /** Sin red o sesión vencida: hay que detener toda la sincronización. */
    data class Stop(val error: AppError) : Step
}

private const val DEFAULT_REJECTED = "El servidor rechazó el registro."

internal fun AppError.messageOrDefault(): String = serverMessage() ?: DEFAULT_REJECTED

/** El viaje tiene un rechazo definitivo pendiente de que el usuario lo corrija. */
internal fun TripEntity.isBlocked(): Boolean =
    syncErrorMessage != null && SyncRejection.isPermanent(syncErrorCode)

// ---------------- Mapeos a los contratos de la API ----------------

internal fun TrackPointEntity.toRequest() = PuntoRutaRequestDto(
    latitud = lat.round7(),
    longitud = lng.round7(),
    timestampCaptura = t,
    precisionMetros = acc?.coerceIn(0.0, 9999.99)?.round2(),
    altitud = alt?.coerceIn(-500.0, 9000.0)?.round2(),
    velocidad = spd?.coerceIn(0.0, 9999.99)?.round2(),
)

internal fun FuelEntity.toForm(
    viajeId: String?,
    motivo: String?,
    receipt: ReceiptUpload?,
    cargaCombustibleId: String? = null,
) = CargaFormData(
    viajeId = viajeId,
    litros = liters,
    monto = amount,
    fechaCarga = loadedAt,
    ubicacionLat = lat,
    ubicacionLng = lng,
    motivo = motivo,
    centroCostoId = costCenter?.id,
    receipt = receipt,
    cargaCombustibleId = cargaCombustibleId,
)

// ---------------- Offline-first: el mismo UUID en el teléfono y en la API ----------------

/** EstadoViaje.EnCurso tal como lo serializa la API (JsonStringEnumConverter en camelCase). */
internal const val ESTADO_EN_CURSO = "enCurso"

/** 409 de la API: el carro ya tiene otro viaje abierto (trae viajeId, empleadoOperadorId y fechaInicio). */
internal const val CODIGO_VIAJE_EN_CURSO = "VIAJE_EN_CURSO"

private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

/**
 * El id local para mandarlo a la API como UUID del registro. Los viajes y recargas ya se crean con
 * Uuid.random(); si por algo no es un UUID válido no se manda (la API generaría uno propio).
 */
internal fun String.asUuidOrNull(): String? =
    takeIf { UUID_REGEX.matches(it) && it != "00000000-0000-0000-0000-000000000000" }

/**
 * Reconoce el viaje del teléfono en un 409 VIAJE_EN_CURSO: mismo operador y misma hora de inicio
 * (±[TOLERANCE_SECONDS]). Sirve para los viajes que una versión anterior de la app creó SIN mandar su
 * UUID y cuya respuesta se perdió: la API los tiene con otro id y sin esto se quedarían atorados.
 */
internal object ServerTripMatch {
    const val TOLERANCE_SECONDS = 120L

    fun isSameTrip(
        employeeId: String,
        startedAt: String,
        serverEmployeeId: String?,
        serverStartedAt: String?,
    ): Boolean {
        if (serverEmployeeId == null || !serverEmployeeId.equals(employeeId, ignoreCase = true)) return false
        val local = wallClockSeconds(startedAt) ?: return false
        val server = serverStartedAt?.let(::wallClockSeconds) ?: return false
        return abs(local - server) <= TOLERANCE_SECONDS
    }

    /**
     * Segundos de la fecha y hora "de pared", sin la zona: "2026-11-04T17:40:00-06:00" (teléfono) y
     * "2026-11-04T17:40:00" o "2026-11-04T17:40:00.123" (API) dan lo mismo. La API (TZ en el contenedor)
     * y los teléfonos trabajan en America/Mexico_City. null si el texto no tiene ese formato.
     */
    fun wallClockSeconds(iso: String): Long? {
        if (iso.length < 19 || iso[4] != '-' || iso[7] != '-' || iso[10] != 'T' || iso[13] != ':' || iso[16] != ':') return null
        val year = iso.substring(0, 4).toIntOrNull() ?: return null
        val month = iso.substring(5, 7).toIntOrNull()?.takeIf { it in 1..12 } ?: return null
        val day = iso.substring(8, 10).toIntOrNull()?.takeIf { it in 1..31 } ?: return null
        val hour = iso.substring(11, 13).toIntOrNull() ?: return null
        val minute = iso.substring(14, 16).toIntOrNull() ?: return null
        val second = iso.substring(17, 19).toIntOrNull() ?: return null
        return daysFromCivil(year, month, day) * 86_400L + hour * 3_600L + minute * 60L + second
    }

    /** Días desde 1970-01-01 (algoritmo days_from_civil de H. Hinnant; sin depender de kotlinx-datetime). */
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yearOfEra = y - era * 400
        val dayOfYear = (153 * ((month + 9) % 12) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146_097L + dayOfEra - 719_468L
    }
}

/**
 * Kilometraje final: el que capturó el usuario en "Registrar Viaje" (odómetro).
 * Si por algún motivo no existe, se estima con el inicial + la distancia del GPS.
 */
internal fun TripEntity.finalMileage(): Double =
    (finalMileage ?: (initialMileage + distanceMeters / 1000.0)).round2()

internal fun PauseEntity.toSyncDto() = PausaSincronizadaDto(
    fechaInicio = startedAt,
    fechaFin = endedAt,
    motivo = reason,
    ubicacionLat = lat?.round7(),
    ubicacionLng = lng?.round7(),
)

internal fun Double.round2() = round(this * 100) / 100
internal fun Double.round7() = round(this * 10_000_000) / 10_000_000
