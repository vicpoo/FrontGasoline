package org.vicpoo.gasolineproject.feature.trips.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
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
 * - refresh() descarga viajes (ya traen quién manejó), las cargas de cada viaje y las rutas.
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

        val trips = when (val result = safeApiCall { api.getTrips(companyId, operatorFilter) }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }

        val cached = cache.current().trips.associateBy { it.id }

        // Cargas de cada viaje (en paralelo). Si una falla se queda la guardada.
        val loadsByTrip = coroutineScope {
            trips.map { trip ->
                async {
                    val loads = safeApiCall { api.getFuelLoads(trip.viajeId) }.getOrNull()?.map {
                        FuelLoadEntity(it.cargaCombustibleId, it.litros, it.monto, it.fotoComprobanteUrl)
                    }
                    trip.viajeId to (loads ?: cached[trip.viajeId]?.fuelLoads.orEmpty())
                }
            }.awaitAll().toMap()
        }

        // Rutas: se reutiliza la guardada SOLO si el viaje ya estaba terminado cuando se guardó.
        // Si estaba en curso, la ruta guardada está incompleta y se vuelve a descargar.
        val routes = coroutineScope {
            trips.map { trip ->
                async {
                    val previous = cached[trip.viajeId]
                    val cached = previous?.route?.takeIf { previous.endedAt != null && trip.fechaFin != null }
                    if (!cached.isNullOrEmpty()) {
                        trip.viajeId to cached
                    } else {
                        val map = safeApiCall { api.getRouteMap(trip.viajeId) }.getOrNull()
                        trip.viajeId to map?.coordenadas.orEmpty().map { listOf(it.latitud, it.longitud) }
                    }
                }
            }.awaitAll().toMap()
        }

        val entities = trips
            .sortedByDescending { AppDates.sortKey(it.fechaInicio) }
            .map { trip ->
                val operator = trip.operador
                val isMine = trip.empleadoOperadorId == session.employeeId
                TripEntity(
                    id = trip.viajeId,
                    operatorName = operator?.nombreCompleto
                        ?: if (isMine) session.name ?: session.email else cached[trip.viajeId]?.operatorName.orEmpty(),
                    operatorPhotoUrl = operator?.fotoPerfilUrl ?: if (isMine) session.photoUrl else cached[trip.viajeId]?.operatorPhotoUrl,
                    startedAt = trip.fechaInicio,
                    endedAt = trip.fechaFin,
                    distanceKm = trip.distanciaRecorrida,
                    finalMileageKm = trip.kilometrajeFinal,
                    status = trip.estado,
                    fuelLoads = loadsByTrip[trip.viajeId].orEmpty(),
                    route = routes[trip.viajeId].orEmpty(),
                    pausedSeconds = trip.segundosEnPausa,
                )
            }

        cache.save(TripsSnapshot(entities))
        return AppResult.Success(Unit)
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
