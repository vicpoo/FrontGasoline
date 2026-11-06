package org.vicpoo.gasolineproject.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.data.profile.ProfileRepositoryImpl
import org.vicpoo.gasolineproject.data.profile.remote.KtorProfileApi
import org.vicpoo.gasolineproject.data.profile.remote.ProfileApi
import org.vicpoo.gasolineproject.domain.profile.ProfileRepository

val profileDataModule = module {
    singleOf(::KtorProfileApi) { bind<ProfileApi>() }
    singleOf(::ProfileRepositoryImpl) { bind<ProfileRepository>() }
}
