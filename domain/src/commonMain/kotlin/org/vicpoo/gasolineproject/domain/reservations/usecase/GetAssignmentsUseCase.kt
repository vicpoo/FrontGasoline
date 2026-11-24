package org.vicpoo.gasolineproject.domain.reservations.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.AssignmentsOverview
import org.vicpoo.gasolineproject.domain.reservations.AssignmentsRepository

class GetAssignmentsUseCase(private val repository: AssignmentsRepository) {
    suspend operator fun invoke(): AppResult<AssignmentsOverview> = repository.load()
}
