package org.vicpoo.gasolineproject.domain.reports.usecase

import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reports.ReportFile
import org.vicpoo.gasolineproject.domain.reports.ReportFilters
import org.vicpoo.gasolineproject.domain.reports.ReportFormat
import org.vicpoo.gasolineproject.domain.reports.ReportsRepository

class DownloadReportUseCase(private val repository: ReportsRepository) {
    suspend operator fun invoke(filters: ReportFilters, format: ReportFormat): AppResult<ReportFile> =
        repository.download(filters, format)
}
