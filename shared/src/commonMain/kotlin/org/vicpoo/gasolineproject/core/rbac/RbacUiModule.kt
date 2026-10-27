package org.vicpoo.gasolineproject.core.rbac

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Parte de RBAC que es de UI (el ViewModel que usa RequirePrivilege).
 * La consulta de privilegios (PermissionChecker, rbacModule) vive en :core:data.
 */
val rbacUiModule = module {
    // Una instancia por regla (la llave del ViewModel es AccessRule.key)
    viewModel { params -> AccessGateViewModel(permissionChecker = get(), rule = params.get()) }
}
