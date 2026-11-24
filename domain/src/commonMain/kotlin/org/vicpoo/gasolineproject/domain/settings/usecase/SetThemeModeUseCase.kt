package org.vicpoo.gasolineproject.domain.settings.usecase

import org.vicpoo.gasolineproject.domain.settings.ThemeMode
import org.vicpoo.gasolineproject.domain.settings.ThemePreferenceStore

class SetThemeModeUseCase(private val store: ThemePreferenceStore) {
    suspend operator fun invoke(mode: ThemeMode) = store.save(mode)
}
