package org.vicpoo.gasolineproject.domain.triprecording.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecord
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordRepository

class ObserveFuelRecordsUseCase(private val repository: FuelRecordRepository) {
    operator fun invoke(tripLocalId: String): Flow<List<FuelRecord>> = repository.observeForTrip(tripLocalId)
}
