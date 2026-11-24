package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordRepository
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync

class DeleteFuelRecordUseCase(
    private val repository: FuelRecordRepository,
    private val sync: TripRecordingSync,
) {
    suspend operator fun invoke(localId: String): AppResult<Unit> {
        val result = repository.delete(localId)
        if (result is AppResult.Success) sync.requestSync()
        return result
    }
}
