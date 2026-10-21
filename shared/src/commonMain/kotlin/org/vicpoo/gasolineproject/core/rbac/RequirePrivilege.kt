package org.vicpoo.gasolineproject.core.rbac

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.map
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
) = RequireAnyPrivilege(setOf(privilege), fallback, content)

/**
 * Muestra [content] si el usuario tiene AL MENOS UNO de los privilegios.
 * Mientras se leen los privilegios guardados no dibuja nada (así no parpadea el [fallback]).
 */
@Composable
fun RequireAnyPrivilege(
    privileges: Set<String>,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val checker = koinInject<PermissionChecker>()
    val allowed: Boolean? by remember(privileges, checker) {
        checker.observePrivileges().map { granted -> privileges.any { it in granted } }
    }.collectAsState(initial = null)
    when (allowed) {
        true -> content()
        false -> fallback()
        null -> Unit
    }
}
