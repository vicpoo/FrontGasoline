package org.vicpoo.gasolineproject.feature.triprecording.data.sync

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.httpCode
import org.vicpoo.gasolineproject.core.common.serverMessage
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.core.sync.PendingSyncTask
import org.vicpoo.gasolineproject.core.sync.SyncRejection
import org.vicpoo.gasolineproject.core.sync.stopsSync
import org.vicpoo.gasolineproject.feature.triprecording.data.local.FuelEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.FuelSyncState
import org.vicpoo.gasolineproject.feature.triprecording.data.local.PauseEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TrackPointEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.CargaFormData
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.FinalizarViajeRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.IniciarViajeRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.PausaSincronizadaDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.PausarViajeRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.ReanudarViajeRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.PuntoRutaRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.ReceiptUpload
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.SincronizarViajeRequestDto
import org.vicpoo.gasolineproject.feature.triprecording.data.remote.TripRecordingApi
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripRecordingSync
import kotlin.math.round

/** Resultado de un paso de la sincronización de un viaje. */
internal sealed interface Step {
    data object Done : Step

    /** La API rechazó este registro: el error quedó guardado en él y se sigue con lo demás. */
    data object Rejected : Step

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

internal fun FuelEntity.toForm(viajeId: String?, motivo: String?, receipt: ReceiptUpload?) = CargaFormData(
    viajeId = viajeId,
    litros = liters,
    monto = amount,
    fechaCarga = loadedAt,
    ubicacionLat = lat,
    ubicacionLng = lng,
    motivo = motivo,
    centroCostoId = costCenter?.id,
    receipt = receipt,
)

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
