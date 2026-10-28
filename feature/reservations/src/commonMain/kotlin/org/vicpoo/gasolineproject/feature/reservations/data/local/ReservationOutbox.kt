package org.vicpoo.gasolineproject.feature.reservations.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.data.JsonFileCache

/** Solicitud de carro hecha sin conexión, esperando a enviarse. */
@Serializable
data class OutgoingRequestEntity(
    val localId: String,
    val vehicleServerId: String,
    val vehicleTitle: String? = null,
    val plates: String? = null,
    val photoUrl: String? = null,
    val reason: String,
    /** Fecha local con zona: "2026-10-19T12:30:15-06:00". */
    val createdAt: String,
    /** Último rechazo de la API (4xx = definitivo, ya no se reenvía sola). */
    val errorCode: Int? = null,
    val errorMessage: String? = null,
)

@Serializable
data class ReservationOutboxSnapshot(
    val requests: List<OutgoingRequestEntity> = emptyList(),
)

/**
 * Cola de solicitudes por enviar. Vive en su propio archivo (no en el de "Mis solicitudes")
 * para que refrescar la lista nunca pise una solicitud recién guardada.
 */
class ReservationOutbox(private val cache: JsonFileCache<ReservationOutboxSnapshot>) {

    private val mutex = Mutex()

    val items: Flow<List<OutgoingRequestEntity>> = cache.data.map { it.requests }

    suspend fun all(): List<OutgoingRequestEntity> = cache.current().requests

    /** Una sola por carro: si ya había una en cola para ese carro, la nueva la reemplaza. */
    suspend fun enqueue(item: OutgoingRequestEntity) = edit { list ->
        list.filterNot { it.vehicleServerId == item.vehicleServerId } + item
    }

    suspend fun remove(localId: String) = edit { list -> list.filterNot { it.localId == localId } }

    suspend fun markError(localId: String, code: Int, message: String?) = edit { list ->
        list.map { if (it.localId == localId) it.copy(errorCode = code, errorMessage = message) else it }
    }

    suspend fun clear() = cache.clear()

    private suspend fun edit(transform: (List<OutgoingRequestEntity>) -> List<OutgoingRequestEntity>) {
        mutex.withLock {
            val current = cache.current()
            cache.save(current.copy(requests = transform(current.requests)))
        }
    }
}
