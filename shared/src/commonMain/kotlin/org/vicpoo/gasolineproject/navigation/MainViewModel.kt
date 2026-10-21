package org.vicpoo.gasolineproject.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.core.rbac.PermissionChecker
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.RefreshPrivilegesUseCase

/**
 * Privilegios del usuario para armar las pestañas (RBAC).
 * Al entrar pregunta a la API si cambiaron (GET /api/auth/privilegios con If-None-Match);
 * sin red se usan los guardados.
 */
class MainViewModel(
    permissionChecker: PermissionChecker,
    private val refreshPrivileges: RefreshPrivilegesUseCase,
) : ViewModel() {

    /** null mientras se leen los privilegios guardados (así no se arma la navegación con datos vacíos). */
    val privileges: StateFlow<Set<String>?> = permissionChecker.observePrivileges()
        .map<Set<String>, Set<String>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { refreshPrivileges() }
    }
}
