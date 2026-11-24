package org.vicpoo.gasolineproject.domain.triprecording.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicleRepository
import org.vicpoo.gasolineproject.domain.triprecording.ReleaseBlock

/** Qué carros no se pueden dejar todavía y por qué (viaje en curso o sin enviar). */
class ObserveReleaseBlocksUseCase(private val repository: AssignedVehicleRepository) {
    operator fun invoke(): Flow<Map<String, ReleaseBlock>> = repository.observeReleaseBlocks()
}
