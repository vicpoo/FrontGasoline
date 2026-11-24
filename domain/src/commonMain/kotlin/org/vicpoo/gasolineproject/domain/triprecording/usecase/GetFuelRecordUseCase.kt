package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.triprecording.FuelRecord
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordRepository

class GetFuelRecordUseCase(private val repository: FuelRecordRepository) {
    suspend operator fun invoke(localId: String): FuelRecord? = repository.get(localId)
}
