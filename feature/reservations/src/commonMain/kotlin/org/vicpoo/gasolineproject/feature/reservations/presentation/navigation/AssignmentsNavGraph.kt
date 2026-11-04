package org.vicpoo.gasolineproject.feature.reservations.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.navigation.privilegedComposable
import org.vicpoo.gasolineproject.core.rbac.AppAccess
import org.vicpoo.gasolineproject.core.ui.adaptive.ListDetailLayout
import org.vicpoo.gasolineproject.core.ui.adaptive.showsListAndDetail
import org.vicpoo.gasolineproject.feature.reservations.presentation.assignmentdetail.AssignmentDetailRoute
import org.vicpoo.gasolineproject.feature.reservations.presentation.assignments.AssignmentsRoute
import org.vicpoo.gasolineproject.feature.reservations.resources.Res
import org.vicpoo.gasolineproject.feature.reservations.resources.assignments_select_hint

/** Subgrafo de la pestaña Asignaciones: carros vinculados a un empleado y su detalle (con "Desvincular"). */
@Serializable
data object AssignmentsGraph

sealed interface AssignmentsDestination {
    @Serializable
    data object List : AssignmentsDestination

    /** Detalle: [key] = "r:<reserva>" (un carro) o "e:<empleado>" (todos sus carros). */
    @Serializable
    data class Detail(val key: String) : AssignmentsDestination
}

/** Igual que Solicitudes: en el teléfono el detalle es otra pantalla; en tablet horizontal, lado a lado. */
fun NavGraphBuilder.assignmentsGraph(navController: NavHostController) {
    navigation<AssignmentsGraph>(startDestination = AssignmentsDestination.List) {
        privilegedComposable<AssignmentsDestination.List>(AppAccess.ASSIGNMENTS) {
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            val twoPanes = showsListAndDetail
            ListDetailLayout(
                selectedId = selected,
                emptyDetailText = stringResource(Res.string.assignments_select_hint),
                detailAccess = AppAccess.ASSIGNMENTS,
                onOpenSinglePane = { key ->
                    selected = null
                    navController.navigate(AssignmentsDestination.Detail(key))
                },
                onCloseDetail = { selected = null },
                list = {
                    AssignmentsRoute(
                        onAssignmentClick = { key ->
                            if (twoPanes) selected = key.value else navController.navigate(AssignmentsDestination.Detail(key.value))
                        },
                    )
                },
                // Al desvincular el último carro, el detalle llama onBack: se limpia el panel
                detail = { key -> AssignmentDetailRoute(key = key, onBack = { selected = null }) },
            )
        }

        privilegedComposable<AssignmentsDestination.Detail>(
            access = AppAccess.ASSIGNMENTS,
            onDenied = { navController.popBackStack() },
        ) { entry ->
            val route = entry.toRoute<AssignmentsDestination.Detail>()
            AssignmentDetailRoute(key = route.key, onBack = { navController.popBackStack() })
        }
    }
}
