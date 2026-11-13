package org.vicpoo.gasolineproject.domain.trips

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.GeoPoint

/** Viaje del historial (ya con su operador, cargas y ruta). */
data class Trip(
    val id: String,
    val operatorName: String,
    val operatorPhotoUrl: String?,
    val startedAt: String,
    val endedAt: String?,
    val distanceKm: Double?,
    val finalMileageKm: Double?,
    val status: TripStatus,
    val fuelLoads: List<FuelLoad>,
    val route: List<GeoPoint>,
    /** Tiempo en pausa: la duración del viaje lo descuenta. */
    val pausedSeconds: Long = 0,
)

data class FuelLoad(
    val id: String,
    val liters: Double,
    val amount: Double?,
    val receiptUrl: String?,
    /** Fecha de la carga (fechaCarga de la API). */
    val date: String? = null,
    /** Dónde se hizo la carga (null si no se sabe): se dibuja en el mapa con el icono de gasolina. */
    val location: GeoPoint? = null,
)

enum class TripStatus {
    IN_PROGRESS, FINISHED, INTERRUPTED, UNKNOWN;

    companion object {
        fun fromApi(value: String?) = when (value?.lowercase()) {
            "encurso" -> IN_PROGRESS
            "finalizado" -> FINISHED
            "interrumpido" -> INTERRUPTED
            else -> UNKNOWN
        }
    }
}

/** Historial de viajes: se lee del caché local (sin red se ve lo último descargado). */
interface TripsRepository {
    fun observeTrips(): Flow<List<Trip>>
    fun observeTrip(id: String): Flow<Trip?>
    suspend fun refresh(): AppResult<Unit>

    /** La API tiene más viajes después de los guardados. */
    fun observeHasMore(): Flow<Boolean>

    /** Pide a la API los 10 viajes siguientes y los agrega al final. */
    suspend fun loadMore(): AppResult<Unit>

    /** Cargas y ruta de esos viajes (el historial las pide por bloques conforme se muestran). */
    suspend fun loadDetails(tripIds: List<String>): AppResult<Unit>
}

class ObserveTripsUseCase(private val repository: TripsRepository) {
    operator fun invoke(): Flow<List<Trip>> = repository.observeTrips()
}

class ObserveTripUseCase(private val repository: TripsRepository) {
    operator fun invoke(id: String): Flow<Trip?> = repository.observeTrip(id)
}

class RefreshTripsUseCase(private val repository: TripsRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}

/** Historial: al bajar al final se piden los 10 viajes siguientes. */
class LoadMoreTripsUseCase(private val repository: TripsRepository) {
    fun hasMore(): Flow<Boolean> = repository.observeHasMore()
    suspend operator fun invoke(): AppResult<Unit> = repository.loadMore()
}

class LoadTripDetailsUseCase(private val repository: TripsRepository) {
    suspend operator fun invoke(tripIds: List<String>): AppResult<Unit> = repository.loadDetails(tripIds)
}
