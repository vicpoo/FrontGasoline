package org.vicpoo.gasolineproject.domain.reports.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reports.ReportFilters
import org.vicpoo.gasolineproject.domain.reports.ReportsRepository

class RefreshReportUseCase(private val repository: ReportsRepository) {
    suspend operator fun invoke(filters: ReportFilters): AppResult<Unit> = repository.refresh(filters)
}
