package org.vicpoo.gasolineproject.data.trips

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.AppDates
import org.vicpoo.gasolineproject.domain.common.getOrNull
import org.vicpoo.gasolineproject.data.local.JsonFileCache
import org.vicpoo.gasolineproject.data.network.LIST_PAGE_SIZE
import org.vicpoo.gasolineproject.data.network.hasMoreAfter
import org.vicpoo.gasolineproject.data.network.refreshSize
import org.vicpoo.gasolineproject.data.network.safeApiCall
import org.vicpoo.gasolineproject.domain.rbac.PermissionChecker
import org.vicpoo.gasolineproject.domain.rbac.Privileges
import org.vicpoo.gasolineproject.domain.session.Session
import org.vicpoo.gasolineproject.domain.session.SessionReader
import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.data.trips.remote.TripsApi
import org.vicpoo.gasolineproject.data.trips.remote.ViajeDto
import org.vicpoo.gasolineproject.domain.trips.FuelLoad
import org.vicpoo.gasolineproject.domain.trips.Trip
import org.vicpoo.gasolineproject.domain.trips.TripStatus
import org.vicpoo.gasolineproject.domain.trips.TripsRepository

// ---------- Lo que se guarda en el caché offline ----------

@Serializable
data class TripsSnapshot(
    val trips: List<TripEntity> = emptyList(),
    /** Páginas de 10 ya pedidas a la API (al recargar se vuelven a pedir todas en una petición). */
    val loadedPages: Int = 0,
    /** La API tiene más viajes después de los guardados. */
    val hasMore: Boolean = false,
)

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
    val date: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
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

    override fun observeHasMore(): Flow<Boolean> = cache.data.map { it.hasMore }.distinctUntilChanged()

    /**
     * Vuelve a pedir los viajes que ya se veían (los 10 más recientes, o más si el usuario ya
     * había bajado) en UNA petición. Las cargas y la ruta se piden aparte con loadDetails().
     */
    override suspend fun refresh(): AppResult<Unit> {
        val session = sessionReader.current() ?: return AppResult.Success(Unit)
        val previous = cache.current()
        val size = refreshSize(previous.loadedPages)
        val page = when (val result = safeApiCall { api.getTrips(session.companyId, operatorFilter(session), page = 1, pageSize = size) }) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val snapshot = TripsSnapshot(
            trips = toEntities(page.items, session, previous.trips.associateBy { it.id }),
            loadedPages = size / LIST_PAGE_SIZE,
            hasMore = hasMoreAfter(size, page.items.size, size, page.total),
        )
        cache.save(snapshot)
        // Si en esos no hubo ningún viaje cerrado (todos en curso) se piden los siguientes
        if (snapshot.trips.isEmpty() && snapshot.hasMore) return loadMore()
        return AppResult.Success(Unit)
    }

    /**
     * El usuario llegó al final del historial: se piden los 10 viajes siguientes a la API y se agregan
     * al final. Si en esa página no hay ningún viaje cerrado se pide la siguiente (hasta que haya uno).
     */
    override suspend fun loadMore(): AppResult<Unit> {
        val session = sessionReader.current() ?: return AppResult.Success(Unit)
        var snapshot = cache.current()
        while (snapshot.hasMore) {
            val nextPage = snapshot.loadedPages + 1
            val page = when (val result = safeApiCall {
                api.getTrips(session.companyId, operatorFilter(session), page = nextPage, pageSize = LIST_PAGE_SIZE)
            }) {
                is AppResult.Failure -> return result
                is AppResult.Success -> result.data
            }
            val known = snapshot.trips.map { it.id }.toSet()
            val added = toEntities(page.items, session, snapshot.trips.associateBy { it.id }).filter { it.id !in known }
            // Se vuelve a leer el caché: pudo cambiar (loadDetails) mientras se descargaba
            val current = cache.current()
            snapshot = current.copy(
                trips = current.trips + added.filter { a -> current.trips.none { it.id == a.id } },
                loadedPages = nextPage,
                hasMore = hasMoreAfter(nextPage * LIST_PAGE_SIZE, page.items.size, LIST_PAGE_SIZE, page.total),
            )
            cache.save(snapshot)
            if (added.isNotEmpty()) break
        }
        return AppResult.Success(Unit)
    }

    /** Con viaje.ver_todos se ven los de toda la empresa; si no, solo los propios. */
    private suspend fun operatorFilter(session: Session): String? =
        if (permissionChecker.has(Privileges.VIAJE_VER_TODOS)) null else session.employeeId

    /**
     * El historial solo muestra viajes cerrados (finalizados o interrumpidos): un viaje en curso
     * o en pausa todavía no termina y su ruta está incompleta. Ese se ve en "Registrar viaje".
     * Las cargas y la ruta NO se piden aquí: mientras tanto se muestran las guardadas (si había).
     */
    private fun toEntities(
        dtos: List<ViajeDto>,
        session: Session,
        cached: Map<String, TripEntity>,
    ): List<TripEntity> =
        dtos.filter { it.isClosed() }
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
                    // Se vuelven a pedir (las cargas pudieron corregirse y la ruta completarse);
                    // mientras tanto se muestra lo guardado
                    detailsLoaded = false,
                )
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
                        FuelLoadEntity(it.cargaCombustibleId, it.litros, it.monto, it.fotoComprobanteUrl, it.fechaCarga, it.ubicacionLat, it.ubicacionLng)
                    }
                    // La ruta se vuelve a pedir en cada actualización: aunque el viaje ya esté cerrado,
                    // pueden llegar después lotes de puntos que estaban pendientes (sin red o error 5xx)
                    // y la API recalcula la ruta. Si falla, se queda la guardada.
                    val route = safeApiCall { api.getRouteMap(trip.id) }.getOrNull()
                        ?.coordenadas?.map { listOf(it.latitud, it.longitud) }
                        ?.takeIf { it.isNotEmpty() }
                    trip.copy(
                        fuelLoads = loads ?: trip.fuelLoads,
                        route = route ?: trip.route,
                        detailsLoaded = loads != null,
                    )
                }
            }.awaitAll()
        }.associateBy { it.id }

        // Se vuelve a leer el caché: pudo cambiar mientras se descargaba
        val latest = cache.current()
        cache.save(latest.copy(trips = latest.trips.map { loaded[it.id] ?: it }))
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
    fuelLoads = fuelLoads.map { FuelLoad(
            id = it.id,
            liters = it.liters,
            amount = it.amount,
            receiptUrl = it.receiptUrl,
            date = it.date,
            location = if (it.lat != null && it.lng != null) GeoPoint(it.lat, it.lng) else null,
        ) },
    route = route.mapNotNull { pair -> if (pair.size >= 2) GeoPoint(pair[0], pair[1]) else null },
    pausedSeconds = pausedSeconds,
)

/** Cerrado = ya tiene fecha de fin y no está "enCurso" (la pausa también cuenta como en curso). */
private fun ViajeDto.isClosed(): Boolean =
    fechaFin != null && !estado.equals("enCurso", ignoreCase = true)
