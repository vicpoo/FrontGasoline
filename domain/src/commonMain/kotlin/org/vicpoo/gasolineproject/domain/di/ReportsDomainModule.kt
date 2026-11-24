package org.vicpoo.gasolineproject.domain.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.reports.usecase.DownloadReportUseCase
import org.vicpoo.gasolineproject.domain.reports.usecase.ObserveReportUseCase
import org.vicpoo.gasolineproject.domain.reports.usecase.ObserveReportVehiclesUseCase
import org.vicpoo.gasolineproject.domain.reports.usecase.RefreshReportUseCase

val reportsDomainModule = module {
    factoryOf(::ObserveReportUseCase)
    factoryOf(::ObserveReportVehiclesUseCase)
    factoryOf(::RefreshReportUseCase)
    factoryOf(::DownloadReportUseCase)
}
