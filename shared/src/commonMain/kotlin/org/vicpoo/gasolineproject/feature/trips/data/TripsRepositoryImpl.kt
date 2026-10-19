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
 * - refresh() descarga viajes, cargas y rutas, y reemplaza el caché.
 * - RBAC: con viaje.ver_todos se ven los viajes de toda la empresa; si no, solo los propios.
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

        // Cargas de la empresa (una sola petición) agrupadas por viaje
        val loadsByTrip = safeApiCall { api.getFuelLoads(companyId) }.getOrNull().orEmpty()
            .groupBy { it.viajeId }

        // Nombres y fotos de los operadores (solo si ve los viajes de otros)
        val employees = if (seeAll && companyId != null) {
            safeApiCall { api.getEmployees(companyId) }.getOrNull().orEmpty().associateBy { it.empleadoId }
        } else {
            emptyMap()
        }

        // Rutas: se reutiliza la guardada; solo se descargan las que faltan (en paralelo)
        val cachedRoutes = cache.current().trips.associate { it.id to it.route }
        val routes = coroutineScope {
            trips.map { trip ->
                async {
                    val cached = cachedRoutes[trip.viajeId]
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
                val employee = employees[trip.empleadoOperadorId]
                val isMine = trip.empleadoOperadorId == session.employeeId
                TripEntity(
                    id = trip.viajeId,
                    operatorName = employee?.nombreCompleto ?: if (isMine) session.name ?: session.email else "",
                    operatorPhotoUrl = employee?.fotoPerfilUrl ?: if (isMine) session.photoUrl else null,
                    startedAt = trip.fechaInicio,
                    endedAt = trip.fechaFin,
                    distanceKm = trip.distanciaRecorrida,
                    finalMileageKm = trip.kilometrajeFinal,
                    status = trip.estado,
                    fuelLoads = loadsByTrip[trip.viajeId].orEmpty().map {
                        FuelLoadEntity(it.cargaCombustibleId, it.litros, it.monto, it.fotoComprobanteUrl)
                    },
                    route = routes[trip.viajeId].orEmpty(),
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
)
