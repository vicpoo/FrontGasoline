package org.vicpoo.gasolineproject.ui.triprecording.syncstatus

/** Lo que hace el usuario en "Envíos pendientes". */
sealed interface SyncStatusEvent {
    data object SendNowClicked : SyncStatusEvent
    data object ResultDismissed : SyncStatusEvent
    data object ToggleFullLog : SyncStatusEvent
}
