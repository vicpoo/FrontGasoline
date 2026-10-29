package org.vicpoo.gasolineproject.feature.trips.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.AppDates
import org.vicpoo.gasolineproject.core.common.getOrNull
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.rbac.PermissionChecker
import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.feature.trips.data.remote.TripsApi
import org.vicpoo.gasolineproject.feature.trips.data.remote.ViajeDto
import org.vicpoo.gasolineproject.feature.trips.domain.FuelLoad
import org.vicpoo.gasolineproject.feature.trips.domain.Trip
import org.vicpoo.gasolineproject.feature.trips.domain.TripStatus
import org.vicpoo.gasolineproject.feature.trips.domain.TripsRepository

// ---------- Lo que se guarda en el caché offline ----------

@Serializable
data class TripsSnapshot(val trips: List<TripEntity> = emptyList())

@Serializable
data class TripEntity(
    val id: String,
    val operatorName: String,
    val operatorPhotoUrl: String? = null,
    val startedAt: String,
    val endedAt: String? = null,
    val distanceKm: Double? = null,
    val finalMileageKm: Double? = null,
    val status: String? = null,
    val fuelLoads: List<FuelLoadEntity> = emptyList(),
    /** Ruta como pares [lat, lng]. */
    val route: List<List<Double>> = emptyList(),
    val pausedSeconds: Long = 0,
    /**
     * true cuando ya se descargaron sus cargas y su ruta en esta actualización.
     * El historial las pide por bloques de 4 conforme el usuario baja (ver loadDetails).
     */
    val detailsLoaded: Boolean = false,
)

@Serializable
data class FuelLoadEntity(
    val id: String,
    val liters: Double,
    val amount: Double? = null,
    val receiptUrl: String? = null,
)

/**
 * Historial de viajes offline-first:
 * - La UI lee del caché (JSON en el teléfono).
 * - refresh() descarga SOLO la lista de viajes (ya traen quién manejó): 1 petición.
 * - loadDetails() descarga cargas y ruta por bloques (los 4 que se ven, luego los siguientes).
 * - RBAC: con viaje.ver_todos se ven los viajes de toda la empresa; si no, solo los propios.
 *   Las cargas se piden por viaje (?viajeId=) porque así las permite la API a quien solo ve sus viajes.
 */
class TripsRepositoryImpl(
    private val api: TripsApi,
    private val cache: JsonFileCache<TripsSnapshot>,
    private val sessionReader: SessionReader,
    private val permissionChecker: PermissionChecker,
) : TripsRepository {

    override fun observeTrips(): Flow<List<Trip>> =
        cache.data.map { snapshot -> snapshot.trips.map { it.toDomain() } }

    override fun observeTrip(id: String): Flow<Trip?> =
        cache.data.map { snapshot -> snapshot.trips.firstOrNull { it.id == id }?.toDomain() }

    override suspend fun refresh(): AppResult<Unit> {
        val session = sessionReader.current() ?: return AppResult.Success(Unit)
        val seeAll = permissionChecker.has(Privileges.VIAJE_VER_TODOS)
        val companyId = session.companyId
        val operatorFilter = if (seeAll) null else session.employeeId

        // El historial solo muestra viajes cerrados (finalizados o interrumpidos): un viaje en curso
        // o en pausa todavía no termina y su ruta está incompleta. Ese se ve en "Registrar viaje".
        val trips = when (val result = safeApiCall { api.getTrips(companyId, operatorFilter) }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data.filter { it.isClosed() }
        }

        val cached = cache.current().trips.associateBy { it.id }

        // Solo la lista (1 petición). Las cargas y la ruta de cada viaje NO se piden aquí:
        // se piden por bloques con loadDetails() cuando el viaje aparece en pantalla.
        // Mientras tanto se muestran las guardadas (si había).
        val entities = trips
            .sortedByDescending { AppDates.sortKey(it.fechaInicio) }
            .map { trip ->
                val operator = trip.operador
                val isMine = trip.empleadoOperadorId == session.employeeId
                val previous = cached[trip.viajeId]
                // La ruta guardada solo sirve si el viaje ya estaba terminado cuando se guardó
                val keptRoute = previous?.route?.takeIf { previous.endedAt != null && trip.fechaFin != null }.orEmpty()
                TripEntity(
                    id = trip.viajeId,
                    operatorName = operator?.nombreCompleto
                        ?: if (isMine) session.name ?: session.email else previous?.operatorName.orEmpty(),
                    operatorPhotoUrl = operator?.fotoPerfilUrl ?: if (isMine) session.photoUrl else previous?.operatorPhotoUrl,
                    startedAt = trip.fechaInicio,
                    endedAt = trip.fechaFin,
                    distanceKm = trip.distanciaRecorrida,
                    finalMileageKm = trip.kilometrajeFinal,
                    status = trip.estado,
                    fuelLoads = previous?.fuelLoads.orEmpty(),
                    route = keptRoute,
                    pausedSeconds = trip.segundosEnPausa,
                    // Se vuelven a pedir (las cargas pudieron corregirse); la ruta guardada se reutiliza
                    detailsLoaded = false,
                )
            }

        cache.save(TripsSnapshot(entities))
        return AppResult.Success(Unit)
    }

    /**
     * Descarga las cargas y la ruta SOLO de [tripIds] (un bloque de 4 del historial, o el viaje
     * que se abrió en el detalle). Los que ya se descargaron en esta actualización se saltan.
     * Si una petición falla se queda lo guardado.
     */
    override suspend fun loadDetails(tripIds: List<String>): AppResult<Unit> {
        val current = cache.current().trips
        val pending = current.filter { it.id in tripIds && !it.detailsLoaded }
        if (pending.isEmpty()) return AppResult.Success(Unit)

        var noConnection = false
        val loaded = coroutineScope {
            pending.map { trip ->
                async {
                    val loadsResult = safeApiCall { api.getFuelLoads(trip.id) }
                    if (loadsResult is AppResult.Failure && loadsResult.error == AppError.NoConnection) noConnection = true
                    val loads = loadsResult.getOrNull()?.map {
                        FuelLoadEntity(it.cargaCombustibleId, it.litros, it.monto, it.fotoComprobanteUrl)
                    }
                    // La ruta de un viaje terminado no cambia: si ya se tiene, no se vuelve a pedir
                    val route = trip.route.takeIf { it.isNotEmpty() && trip.endedAt != null }
                        ?: safeApiCall { api.getRouteMap(trip.id) }.getOrNull()
                            ?.coordenadas?.map { listOf(it.latitud, it.longitud) }
                    trip.copy(
                        fuelLoads = loads ?: trip.fuelLoads,
                        route = route ?: trip.route,
                        detailsLoaded = loads != null,
                    )
                }
            }.awaitAll()
        }.associateBy { it.id }

        // Se vuelve a leer el caché: pudo cambiar mientras se descargaba
        cache.save(TripsSnapshot(cache.current().trips.map { loaded[it.id] ?: it }))
        return if (noConnection) AppResult.Failure(AppError.NoConnection) else AppResult.Success(Unit)
    }

    suspend fun clear() = cache.clear()
}

private fun TripEntity.toDomain() = Trip(
    id = id,
    operatorName = operatorName,
    operatorPhotoUrl = operatorPhotoUrl,
    startedAt = startedAt,
    endedAt = endedAt,
    distanceKm = distanceKm,
    finalMileageKm = finalMileageKm,
    status = TripStatus.fromApi(status),
    fuelLoads = fuelLoads.map { FuelLoad(it.id, it.liters, it.amount, it.receiptUrl) },
    route = route.mapNotNull { pair -> if (pair.size >= 2) GeoPoint(pair[0], pair[1]) else null },
    pausedSeconds = pausedSeconds,
)

/** Cerrado = ya tiene fecha de fin y no está "enCurso" (la pausa también cuenta como en curso). */
private fun ViajeDto.isClosed(): Boolean =
    fechaFin != null && !estado.equals("enCurso", ignoreCase = true)
