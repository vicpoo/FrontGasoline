package org.vicpoo.gasolineproject.navigation.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.navigation.MainViewModel
import org.vicpoo.gasolineproject.navigation.StartViewModel

val navigationModule = module {
    viewModelOf(::StartViewModel)
    viewModelOf(::MainViewModel)
}
