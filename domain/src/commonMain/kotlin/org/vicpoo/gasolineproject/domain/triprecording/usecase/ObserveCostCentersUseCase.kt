package org.vicpoo.gasolineproject.domain.triprecording.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.triprecording.CostCenter
import org.vicpoo.gasolineproject.domain.triprecording.CostCenterRepository

class ObserveCostCentersUseCase(private val repository: CostCenterRepository) {
    operator fun invoke(): Flow<List<CostCenter>> = repository.observe()
}
