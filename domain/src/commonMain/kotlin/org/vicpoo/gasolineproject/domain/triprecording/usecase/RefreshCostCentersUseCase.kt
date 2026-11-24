package org.vicpoo.gasolineproject.domain.triprecording.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.triprecording.CostCenterRepository

class RefreshCostCentersUseCase(private val repository: CostCenterRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}
