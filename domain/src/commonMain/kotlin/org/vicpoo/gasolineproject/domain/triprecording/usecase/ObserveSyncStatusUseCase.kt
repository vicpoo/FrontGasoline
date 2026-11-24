package org.vicpoo.gasolineproject.domain.triprecording.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.triprecording.SyncStatus
import org.vicpoo.gasolineproject.domain.triprecording.SyncStatusRepository

class ObserveSyncStatusUseCase(private val repository: SyncStatusRepository) {
    operator fun invoke(): Flow<SyncStatus> = repository.observe()
}
