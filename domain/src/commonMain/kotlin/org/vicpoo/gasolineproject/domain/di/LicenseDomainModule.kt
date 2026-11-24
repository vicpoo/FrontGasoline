package org.vicpoo.gasolineproject.domain.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.license.usecase.ObserveMustUploadLicenseUseCase
import org.vicpoo.gasolineproject.domain.license.usecase.UploadLicenseUseCase

val licenseDomainModule = module {
    factoryOf(::UploadLicenseUseCase)
    factoryOf(::ObserveMustUploadLicenseUseCase)
}
