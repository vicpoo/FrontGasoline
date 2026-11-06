package org.vicpoo.gasolineproject.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.local.JsonFileCache
import org.vicpoo.gasolineproject.data.reports.ReportsRepositoryImpl
import org.vicpoo.gasolineproject.data.reports.ReportsSnapshot
import org.vicpoo.gasolineproject.data.reports.remote.KtorReportsApi
import org.vicpoo.gasolineproject.data.reports.remote.ReportsApi
import org.vicpoo.gasolineproject.domain.reports.ReportsRepository
import org.vicpoo.gasolineproject.domain.session.LogoutListener

private val REPORTS_CACHE = named("reportsCache")

val reportsDataModule = module {
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
}
