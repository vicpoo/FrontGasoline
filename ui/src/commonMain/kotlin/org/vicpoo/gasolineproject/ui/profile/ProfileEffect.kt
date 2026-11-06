package org.vicpoo.gasolineproject.ui.profile

sealed interface ProfileEffect {
    data object LoggedOut : ProfileEffect
}
