package org.vicpoo.gasolineproject.core.rbac

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Estado de una "puerta" RBAC: null mientras se leen los privilegios guardados,
 * luego true / false. La UI ([RequireAccess]) solo dibuja según este estado (MVVM).
 */
class AccessGateViewModel(
    permissionChecker: PermissionChecker,
    rule: AccessRule,
) : ViewModel() {

    val allowed: StateFlow<Boolean?> = permissionChecker.observe(rule)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
