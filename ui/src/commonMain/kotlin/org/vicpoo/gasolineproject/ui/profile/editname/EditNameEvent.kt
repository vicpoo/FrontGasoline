package org.vicpoo.gasolineproject.ui.profile.editname

sealed interface EditNameEvent {
    data class NameChanged(val value: String) : EditNameEvent
    data object Save : EditNameEvent
    data object DismissError : EditNameEvent
}
