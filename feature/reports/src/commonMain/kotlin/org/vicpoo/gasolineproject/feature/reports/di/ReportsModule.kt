package org.vicpoo.gasolineproject.feature.reports.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.feature.reports.data.ReportsRepositoryImpl
import org.vicpoo.gasolineproject.feature.reports.data.ReportsSnapshot
import org.vicpoo.gasolineproject.feature.reports.data.remote.KtorReportsApi
import org.vicpoo.gasolineproject.feature.reports.data.remote.ReportsApi
import org.vicpoo.gasolineproject.feature.reports.domain.DownloadReportUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.ObserveReportUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.ObserveReportVehiclesUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.RefreshReportUseCase
import org.vicpoo.gasolineproject.feature.reports.domain.ReportsRepository
import org.vicpoo.gasolineproject.feature.reports.presentation.ReportsViewModel

private val REPORTS_CACHE = named("reportsCache")

val reportsModule = module {
    // Data
    singleOf(::KtorReportsApi) { bind<ReportsApi>() }
    single(REPORTS_CACHE) {
        JsonFileCache(
            fileSystem = get(),
            directories = get(),
            fileName = "reports.json",
            serializer = ReportsSnapshot.serializer(),
            json = get(),
            emptyValue = ReportsSnapshot(),
        )
    }
    single { ReportsRepositoryImpl(api = get(), cache = get(REPORTS_CACHE), clock = get()) }
    single<ReportsRepository> { get<ReportsRepositoryImpl>() }

    // Al cerrar sesión se borra el último reporte guardado (son datos de la empresa)
    single<LogoutListener>(named("reports")) {
        val repository = get<ReportsRepositoryImpl>()
        LogoutListener { repository.clear() }
    }

    // Domain
    factoryOf(::ObserveReportUseCase)
    factoryOf(::ObserveReportVehiclesUseCase)
    factoryOf(::RefreshReportUseCase)
    factoryOf(::DownloadReportUseCase)

    // Presentation
    viewModelOf(::ReportsViewModel)
}
