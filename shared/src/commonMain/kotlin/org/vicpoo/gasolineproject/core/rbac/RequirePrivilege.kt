package org.vicpoo.gasolineproject.core.rbac

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.koinInject

/**
 * Muestra [content] solo si el usuario tiene el privilegio.
 * Uso:
 *   RequirePrivilege(Privileges.CARRO_CREAR) { PrimaryButton("Agregar", ...) }
 */
@Composable
fun RequirePrivilege(
    privilege: String,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val checker = koinInject<PermissionChecker>()
    val allowed by remember(privilege, checker) { checker.observe(privilege) }.collectAsState(initial = false)
    if (allowed) content() else fallback()
}
