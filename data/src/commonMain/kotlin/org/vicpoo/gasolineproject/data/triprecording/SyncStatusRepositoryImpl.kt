package org.vicpoo.gasolineproject.data.triprecording

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.vicpoo.gasolineproject.domain.sync.SyncRejection
import org.vicpoo.gasolineproject.data.triprecording.local.FuelEntity
import org.vicpoo.gasolineproject.data.triprecording.local.FuelSyncState
import org.vicpoo.gasolineproject.data.triprecording.local.RecordingSnapshot
import org.vicpoo.gasolineproject.data.triprecording.local.TripEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.data.triprecording.sync.TripRecordingSyncer
import org.vicpoo.gasolineproject.data.triprecording.sync.TripSyncLog
import org.vicpoo.gasolineproject.domain.triprecording.PendingFuelStatus
import org.vicpoo.gasolineproject.domain.triprecording.PendingTripState
import org.vicpoo.gasolineproject.domain.triprecording.PendingTripStatus
import org.vicpoo.gasolineproject.domain.triprecording.RecordSyncState
import org.vicpoo.gasolineproject.domain.triprecording.SyncStatus
import org.vicpoo.gasolineproject.domain.triprecording.SyncStatusRepository

/** Arma el diagnóstico con lo guardado en el teléfono, la bitácora y si hay un envío en curso. */
class SyncStatusRepositoryImpl(
    private val store: TripRecordingLocalStore,
    private val log: TripSyncLog,
    private val syncer: TripRecordingSyncer,
) : SyncStatusRepository {

    override fun observe(): Flow<SyncStatus> =
        combine(store.snapshot, log.entries, syncer.isSyncing) { snapshot, entries, syncing ->
            SyncStatus(trips = pendingTrips(snapshot), log = entries, isSyncing = syncing)
        }

    private suspend fun pendingTrips(snapshot: RecordingSnapshot): List<PendingTripStatus> =
        snapshot.trips
            .sortedByDescending { it.startedAtMillis }
            .map { trip ->
                val pointsPending = (store.countPoints(trip.localId) - trip.pointsSent).coerceAtLeast(0)
                PendingTripStatus(
                    localId = trip.localId,
                    vehicleTitle = "${trip.vehicle.brand} ${trip.vehicle.model}".trim(),
                    plates = trip.vehicle.plates,
                    startedAt = trip.startedAt,
                    state = trip.pendingState(),
                    registeredOnServer = trip.serverId != null,
                    closedOnServer = trip.closedOnServer,
                    errorMessage = trip.syncErrorMessage,
                    pointsPending = pointsPending,
                    pausesPending = if (trip.closedOnServer) 0 else trip.pauses.count { !it.startSent || (!it.isOpen && !it.endSent) },
                    odometerPhotoPending = trip.odometerPhotoPath != null && !trip.odometerPhotoSent,
                    fuel = snapshot.fuel
                        .filter { it.tripLocalId == trip.localId && (it.state != FuelSyncState.SYNCED || it.syncErrorMessage != null) }
                        .sortedBy { it.number }
                        .map { it.toStatus() },
                )
            }

    private fun TripEntity.pendingState(): PendingTripState = when {
        running -> PendingTripState.IN_PROGRESS
        syncErrorMessage != null && SyncRejection.isPermanent(syncErrorCode) -> PendingTripState.NEEDS_ATTENTION
        else -> PendingTripState.WAITING_TO_SEND
    }

    private fun FuelEntity.toStatus() = PendingFuelStatus(
        localId = localId,
        number = number,
        liters = liters,
        amount = amount,
        state = when {
            syncErrorMessage != null -> RecordSyncState.ERROR
            state == FuelSyncState.SYNCED -> RecordSyncState.SYNCED
            else -> RecordSyncState.PENDING
        },
        errorMessage = syncErrorMessage,
    )
}
