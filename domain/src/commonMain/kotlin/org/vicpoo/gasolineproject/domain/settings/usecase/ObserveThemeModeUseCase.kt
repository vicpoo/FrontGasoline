package org.vicpoo.gasolineproject.domain.settings.usecase

import kotlinx.coroutines.flow.Flow
import org.vicpoo.gasolineproject.domain.settings.ThemeMode
import org.vicpoo.gasolineproject.domain.settings.ThemePreferenceStore

class ObserveThemeModeUseCase(private val store: ThemePreferenceStore) {
    operator fun invoke(): Flow<ThemeMode> = store.observe()
}
