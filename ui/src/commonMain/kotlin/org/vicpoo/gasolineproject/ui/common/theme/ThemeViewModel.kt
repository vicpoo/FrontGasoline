package org.vicpoo.gasolineproject.ui.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.vicpoo.gasolineproject.domain.settings.ObserveThemeModeUseCase
import org.vicpoo.gasolineproject.domain.settings.ThemeMode

/** Apariencia de toda la app (la elige el usuario en Perfil). La usa App() para armar AppTheme. */
class ThemeViewModel(observeThemeMode: ObserveThemeModeUseCase) : ViewModel() {

    /** null mientras se lee lo guardado (es un instante y lo tapa el logo de inicio). */
    val themeMode: StateFlow<ThemeMode?> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/** true = pintar con los colores oscuros. SYSTEM (o aún sin leer) sigue al teléfono. */
@Composable
fun ThemeMode?.isDarkTheme(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
}
