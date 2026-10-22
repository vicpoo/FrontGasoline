package org.vicpoo.gasolineproject.core.rbac

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Muestra [content] solo si el usuario cumple la [rule] (ver [AppAccess]).
 * Mientras se leen los privilegios guardados no dibuja nada (así no parpadea el [fallback]).
 * El estado vive en [AccessGateViewModel]; este composable no consulta datos (MVVM).
 *
 * Uso:
 *   RequireAccess(AppAccess.MY_RESERVATIONS) { ReservationsBell(...) }
 */
@Composable
fun RequireAccess(
    rule: AccessRule,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val viewModel = koinViewModel<AccessGateViewModel>(key = "access-gate:${rule.key}") { parametersOf(rule) }
    val allowed by viewModel.allowed.collectAsStateWithLifecycle()
    when (allowed) {
        true -> content()
        false -> fallback()
        null -> Unit
    }
}

/** Atajo: un solo privilegio. */
@Composable
fun RequirePrivilege(
    privilege: String,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) = RequireAccess(AccessRule.anyOf(privilege), fallback, content)
