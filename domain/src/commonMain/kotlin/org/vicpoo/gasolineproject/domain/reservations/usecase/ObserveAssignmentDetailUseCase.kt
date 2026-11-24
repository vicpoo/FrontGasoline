package org.vicpoo.gasolineproject.domain.reservations.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.reservations.AssignmentDetail
import org.vicpoo.gasolineproject.domain.reservations.AssignmentKey
import org.vicpoo.gasolineproject.domain.reservations.AssignmentsRepository

/** Detalle de una asignación o de un empleado, sobre lo último que se cargó (null si ya no existe). */
class ObserveAssignmentDetailUseCase(private val repository: AssignmentsRepository) {
    operator fun invoke(key: AssignmentKey): Flow<AssignmentDetail?> =
        repository.observe().map { overview -> overview?.detail(key) }
}
