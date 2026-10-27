package org.vicpoo.gasolineproject.core.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.vicpoo.gasolineproject.core.rbac.AccessRule
import org.vicpoo.gasolineproject.core.rbac.RequireAccess
import org.vicpoo.gasolineproject.core.ui.components.AccessDeniedView

/**
 * composable<T> protegido por RBAC: si el usuario no cumple la [access] (ver AppAccess)
 * se muestra "No tienes permiso" en lugar de la pantalla. Así una ruta no se puede abrir
 * aunque alguien navegue a ella directamente (las pestañas y botones ya se ocultan aparte).
 */
inline fun <reified T : Any> NavGraphBuilder.privilegedComposable(
    access: AccessRule,
    noinline onDenied: (() -> Unit)? = null,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        RequireAccess(
            rule = access,
            fallback = { AccessDeniedView(onBack = onDenied) },
        ) { content(this, entry) }
    }
}

/** Navega y borra todo el historial (ej. después de iniciar o cerrar sesión). */
fun NavHostController.navigateClearingBackStack(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
