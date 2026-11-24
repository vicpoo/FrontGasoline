package org.vicpoo.gasolineproject.domain.reports.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.reports.ReportVehicleOption
import org.vicpoo.gasolineproject.domain.reports.ReportsRepository

class ObserveReportVehiclesUseCase(private val repository: ReportsRepository) {
    operator fun invoke(): Flow<List<ReportVehicleOption>> = repository.observeVehicles()
}
