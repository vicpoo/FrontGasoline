package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync

/** Manda en segundo plano lo que haya quedado pendiente (viajes, puntos, recargas). */
class SyncPendingTripsUseCase(private val sync: TripRecordingSync) {
    operator fun invoke() = sync.requestSync()
}
