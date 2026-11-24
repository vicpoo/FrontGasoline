package org.vicpoo.gasolineproject.domain.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.settings.usecase.ObserveThemeModeUseCase
import org.vicpoo.gasolineproject.domain.settings.usecase.SetThemeModeUseCase

/** Apariencia (claro / oscuro / igual que el teléfono) que se elige en Perfil. */
val settingsDomainModule = module {
    factoryOf(::ObserveThemeModeUseCase)
    factoryOf(::SetThemeModeUseCase)
}
