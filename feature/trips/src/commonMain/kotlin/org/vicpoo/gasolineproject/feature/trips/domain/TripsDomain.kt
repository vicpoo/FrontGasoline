package org.vicpoo.gasolineproject.feature.trips.domain

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.GeoPoint

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

    /** Cargas y ruta de esos viajes (el historial las pide de 4 en 4). */
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

class LoadTripDetailsUseCase(private val repository: TripsRepository) {
    suspend operator fun invoke(tripIds: List<String>): AppResult<Unit> = repository.loadDetails(tripIds)
}
