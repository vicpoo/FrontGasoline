package org.vicpoo.gasolineproject.domain.reports.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.reports.ReportsRepository
import org.vicpoo.gasolineproject.domain.reports.SavedReport

class ObserveReportUseCase(private val repository: ReportsRepository) {
    operator fun invoke(): Flow<SavedReport?> = repository.observeReport()
}
