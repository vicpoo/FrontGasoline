package org.vicpoo.gasolineproject.ui.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.domain.settings.ObserveThemeModeUseCase
import org.vicpoo.gasolineproject.domain.settings.SetThemeModeUseCase
import org.vicpoo.gasolineproject.domain.settings.ThemeMode

/** Apariencia de toda la app. La usa App() para armar AppTheme. Arranca igual que el teléfono. */
class ThemeViewModel(
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : ViewModel() {

    /** null mientras se lee lo guardado (es un instante y lo tapa el logo de inicio). */
    val themeMode: StateFlow<ThemeMode?> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** El teléfono cambió de claro a oscuro (o al revés): se deja de forzar el modo del botón de Perfil. */
    fun followSystem() {
        viewModelScope.launch { setThemeMode(ThemeMode.SYSTEM) }
    }
}

/** true = pintar con los colores oscuros. SYSTEM (o aún sin leer) sigue al teléfono. */
@Composable
fun ThemeMode?.isDarkTheme(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
}
