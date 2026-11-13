package org.vicpoo.gasolineproject.ui.common.theme

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.settings.ObserveThemeModeUseCase
import org.vicpoo.gasolineproject.domain.settings.SetThemeModeUseCase
import org.vicpoo.gasolineproject.domain.settings.ThemeMode
import org.vicpoo.gasolineproject.testing.FakeThemePreferenceStore
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Apariencia: lo que el usuario elige en Perfil llega a toda la app. */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest : MainDispatcherTest() {

    @Test
    fun followsThePhoneByDefaultAndChangesWhenTheUserPicksAMode() = runTest {
        val store = FakeThemePreferenceStore()
        val setThemeMode = SetThemeModeUseCase(store)
        val viewModel = ThemeViewModel(ObserveThemeModeUseCase(store))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.themeMode.collect {} }

        // Sin elegir nada: igual que el teléfono
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)

        // Elegir de nuevo el modo del teléfono: vuelve a seguirlo
        setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    }
}
